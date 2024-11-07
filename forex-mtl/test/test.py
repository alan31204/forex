import os
import subprocess as sp
import json
import time

ENDPOINT = "localhost:8081"


def test(from_currecy, to_currency):
    # fr = "JPY"
    # to = "USD"
    cmd = f"curl '{ENDPOINT}/rates?from={from_currecy}&to={to_currency}'"
    print(cmd)
    test_res = sp.run(cmd,stdout=sp.PIPE, check=True,shell=True)
    print(test_res.stdout)
    res = json.loads(test_res.stdout)
    return res


test("JPY","USD")['timestamp']
time.sleep(1)
test("JPY","USD")['timestamp']