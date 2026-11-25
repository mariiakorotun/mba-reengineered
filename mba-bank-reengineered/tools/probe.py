"""Функціональний і навантажувальний зонд для MBA Bank (до/після реінжинірингу).

Використання: python3 probe.py http://localhost:8081 out.json
Працює з обома версіями, бо нова версія зберігає ті самі URL.
"""
import json
import re
import statistics
import sys
import time
from concurrent.futures import ThreadPoolExecutor

import requests
from bs4 import BeautifulSoup

BASE = sys.argv[1].rstrip("/")
OUT = sys.argv[2]
STRICT = "--strict" in sys.argv  # ненульовий код виходу, якщо знайдено дефект (для CI)
USER = ("user", "uSe%r456")
ADMIN = ("admin", "@Dm1n789")


def csrf(sess, path):
    r = sess.get(BASE + path)
    m = re.search(r'name="_csrf"\s+value="([^"]+)"', r.text) or re.search(r'value="([^"]+)"\s+name="_csrf"', r.text)
    return m.group(1) if m else None


def login(creds):
    s = requests.Session()
    tok = csrf(s, "/login")
    r = s.post(BASE + "/login", data={"username": creds[0], "password": creds[1], "_csrf": tok}, allow_redirects=False)
    assert "error" not in r.headers.get("Location", ""), f"login failed for {creds[0]}"
    s.tok = csrf(s, "/dashboard")
    return s


def post(sess, path, data, **kw):
    d = dict(data)
    d["_csrf"] = sess.tok
    return sess.post(BASE + path, data=d, allow_redirects=False, **kw)


def accounts(sess):
    """Повертає {id: balance} з /accounts (обидві версії показують 'ID: n')."""
    soup = BeautifulSoup(sess.get(BASE + "/accounts").text, "html.parser")
    res = {}
    for card in soup.select(".account-card"):
        m = re.search(r"ID:\s*(\d+)", card.get_text(" "))
        bal = card.select_one(".balance-value span")
        if m and bal:
            res[int(m.group(1))] = float(bal.get_text().replace(",", ".").replace("\xa0", "").replace(" ", ""))
    return res


def admin_create(admin, user_id, currency="UAH", **kw):
    return post(admin, "/admin/accounts/create", {"userId": user_id, "currency": currency, "type": "CURRENT"}, **kw)


def tx_count(sess, acc_id):
    soup = BeautifulSoup(sess.get(BASE + "/transactions").text, "html.parser")
    n = 0
    for row in soup.select("tbody tr"):
        badge = row.select_one("span.badge")
        if badge and badge.get_text().strip().startswith(f"#{acc_id} "):
            n += 1
    return n


results = {}
user = login(USER)
admin = login(ADMIN)

# ID користувачів: user=1, admin=2 (DataInitializer)
before = set(accounts(user))
admin_create(admin, 1, "UAH")
acc_user = (set(accounts(user)) - before).pop()
before_a = set(accounts(admin))
admin_create(admin, 2, "UAH")
acc_admin = (set(accounts(admin)) - before_a).pop()
post(admin, f"/accounts/{acc_admin}/deposit", {"amount": "500"})

# T1: IDOR — клієнт знімає кошти з чужого рахунку
post(user, f"/accounts/{acc_admin}/withdraw", {"amount": "100"})
bal = accounts(admin).get(acc_admin)
results["T1_idor_withdraw_foreign"] = {"admin_balance_after": bal, "vulnerable": bal != 500.0}

# T2: IDOR — клієнт поповнює чужий рахунок (запис транзакції від чужого імені)
post(user, f"/accounts/{acc_admin}/deposit", {"amount": "1"})
bal2 = accounts(admin).get(acc_admin)
results["T2_idor_deposit_foreign"] = {"admin_balance_after": bal2, "vulnerable": bal2 != bal}

# T3: сторінка деталей рахунку
r = user.get(BASE + f"/accounts/{acc_user}")
results["T3_account_details_status"] = r.status_code

# T4: невалідна валюта
before_a = set(accounts(admin))
admin_create(admin, 2, "BTC")
created = set(accounts(admin)) - before_a
results["T4_invalid_currency_accepted"] = bool(created)

# T5: відкритий редирект через заголовок Referer
r = admin_create(admin, 2, "UAH", headers={"Referer": "https://evil.example/phish"})
results["T5_open_redirect"] = {"location": r.headers.get("Location"),
                               "vulnerable": "evil.example" in (r.headers.get("Location") or "")}

# T6: сума з трьома знаками після коми
b0 = accounts(user)[acc_user]
post(user, f"/accounts/{acc_user}/deposit", {"amount": "0.005"})
b1 = accounts(user)[acc_user]
results["T6_amount_scale"] = {"before": b0, "after": b1, "accepted_invalid_scale": b1 != b0 or tx_count(user, acc_user) > 0}

# T7: гонка — 20 паралельних зняттів по 100 з балансу 1000
post(user, f"/accounts/{acc_user}/deposit", {"amount": "1000"})
start_bal = accounts(user)[acc_user]
tx_before = tx_count(user, acc_user)
def w(_):
    s = login(USER)
    return post(s, f"/accounts/{acc_user}/withdraw", {"amount": "100"}).status_code
with ThreadPoolExecutor(max_workers=20) as ex:
    list(ex.map(w, range(20)))
end_bal = accounts(user)[acc_user]
withdrawn_tx = tx_count(user, acc_user) - tx_before
results["T7_race_condition"] = {
    "start_balance": start_bal, "end_balance": end_bal, "withdraw_tx_recorded": withdrawn_tx,
    "expected_end_balance": round(start_bal - 100 * withdrawn_tx, 2),
    "invariant_broken": abs(end_bal - (start_bal - 100 * withdrawn_tx)) > 0.001 or end_bal < 0,
}

# T8: клієнт закриває ЧУЖИЙ рахунок
r = post(user, "/accounts/close", {"accountId": acc_admin, "accountLabel": "x",
                                     "adminEmail": "attacker@example.com", "closureReason": "test"})
exists = acc_admin in accounts(admin)
results["T8_close_foreign_account"] = {"status": r.status_code, "foreign_account_deleted": not exists}

# T9: закриття власного рахунку з ненульовим балансом
admin_create(admin, 1, "USD")
accs = accounts(user)
acc2 = max(accs)
post(user, f"/accounts/{acc2}/deposit", {"amount": "250"})
tx_b = tx_count(user, acc2)
post(user, "/accounts/close", {"accountId": acc2, "accountLabel": "x",
                                 "adminEmail": "admin@bank.local", "closureReason": "test"})
results["T9_close_nonzero_balance"] = {"deleted_with_money": acc2 not in accounts(user),
                                       "history_lost": tx_count(user, acc2) < tx_b}

# Продуктивність: засіяти історію і заміряти час відповіді
for _ in range(150):
    post(user, f"/accounts/{acc_user}/deposit", {"amount": "10"})
perf = {}
for path, sess in [("/dashboard", user), ("/accounts", user), ("/transactions", user),
                   ("/transactions", admin), ("/dashboard", admin)]:
    for _ in range(10):
        sess.get(BASE + path)
    t = []
    for _ in range(100):
        t0 = time.perf_counter(); sess.get(BASE + path); t.append((time.perf_counter() - t0) * 1000)
    t.sort()
    perf[f"{path} ({'admin' if sess is admin else 'user'})"] = {
        "median_ms": round(statistics.median(t), 2), "p95_ms": round(t[94], 2)}
t = []
for _ in range(100):
    t0 = time.perf_counter(); post(user, f"/accounts/{acc_user}/deposit", {"amount": "1"}); t.append((time.perf_counter() - t0) * 1000)
t.sort()
perf["POST deposit"] = {"median_ms": round(statistics.median(t), 2), "p95_ms": round(t[94], 2)}
results["performance"] = perf

defects = {
    "T1 IDOR: зняття з чужого рахунку": results["T1_idor_withdraw_foreign"]["vulnerable"],
    "T2 IDOR: поповнення чужого рахунку": results["T2_idor_deposit_foreign"]["vulnerable"],
    "T3 Сторінка рахунку не відкривається": results["T3_account_details_status"] != 200,
    "T4 Приймається довільна валюта": results["T4_invalid_currency_accepted"],
    "T5 Відкритий редирект": results["T5_open_redirect"]["vulnerable"],
    "T6 Сума з 3 знаками після коми": results["T6_amount_scale"]["accepted_invalid_scale"],
    "T7 Гонка при паралельному знятті": results["T7_race_condition"]["invariant_broken"],
    "T8 Закриття чужого рахунку": results["T8_close_foreign_account"]["foreign_account_deleted"],
    "T9 Закриття рахунку з коштами": results["T9_close_nonzero_balance"]["deleted_with_money"],
}
results["summary"] = {k: ("ДЕФЕКТ" if v else "OK") for k, v in defects.items()}
json.dump(results, open(OUT, "w"), ensure_ascii=False, indent=2)
print(json.dumps(results, ensure_ascii=False, indent=2))
if STRICT and any(defects.values()):
    sys.exit(1)
