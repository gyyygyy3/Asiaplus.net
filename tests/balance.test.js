"use strict";
const assert = require("node:assert/strict");
process.env.ASIA_PHONE_API_KEY = "2a42f7bb9c14530e8d9ae0779f3fe89ce8a2f768bb301ea7";
process.env.ASIA_PHONE_USSD_CODE = "*350#";
const { handler, _test } = require("../netlify/functions/balance");

async function call(balanceText, key = process.env.ASIA_PHONE_API_KEY) {
  return handler({
    httpMethod: "POST",
    headers: key ? { "x-asiaphone-key": key } : {},
    body: JSON.stringify({ balanceText })
  });
}

(async () => {
  assert.equal(_test.isValidUssdCode("*350#"), true);
  assert.equal(_test.isValidUssdCode("*123*45#"), true);
  assert.equal(_test.isValidUssdCode("350"), false);
  assert.equal(_test.isValidUssdCode("*350"), false);
  assert.equal(_test.isValidUssdCode("*abc#"), false);

  const response = await call("رصيدك هو: 6,700 د.ع");
  assert.equal(response.statusCode, 200);
  assert.deepEqual(JSON.parse(response.body), { ussdCode: "*350#" });

  // Balance content is passed through as input only; no amount calculation or recipient logic.
  const differentBalance = await call("رصيدك الحالي ٤٣٠٠ دينار");
  assert.equal(differentBalance.statusCode, 200);
  assert.deepEqual(JSON.parse(differentBalance.body), { ussdCode: "*350#" });

  const unauthorized = await call("رصيدك 6700", "wrong-key");
  assert.equal(unauthorized.statusCode, 401);

  const savedKey = process.env.ASIA_PHONE_API_KEY;
  delete process.env.ASIA_PHONE_API_KEY;
  const unconfiguredKey = await handler({
    httpMethod: "POST",
    headers: { "x-asiaphone-key": savedKey },
    body: JSON.stringify({ balanceText: "رصيدك 6700" })
  });
  assert.equal(unconfiguredKey.statusCode, 503);
  process.env.ASIA_PHONE_API_KEY = savedKey;

  const savedCode = process.env.ASIA_PHONE_USSD_CODE;
  delete process.env.ASIA_PHONE_USSD_CODE;
  const unconfiguredCode = await call("رصيدك 6700");
  assert.equal(unconfiguredCode.statusCode, 503);
  process.env.ASIA_PHONE_USSD_CODE = savedCode;

  const invalidBody = await handler({ httpMethod: "POST", headers: { "x-asiaphone-key": savedKey }, body: "not-json" });
  assert.equal(invalidBody.statusCode, 400);
  const emptyBalance = await call("   ");
  assert.equal(emptyBalance.statusCode, 400);

  const options = await handler({ httpMethod: "OPTIONS", headers: {}, body: "" });
  assert.equal(options.statusCode, 204);
  assert.match(options.headers["Access-Control-Allow-Headers"], /X-AsiaPhone-Key/);

  console.log("All AsiaPhone receive-balance/return-service-code tests passed.");
})().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
