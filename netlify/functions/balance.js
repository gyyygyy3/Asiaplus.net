"use strict";

const { timingSafeEqual } = require("node:crypto");

function jsonResponse(statusCode, body, headers) {
  return { statusCode, headers, body: JSON.stringify(body) };
}

function getHeader(headers, wantedName) {
  const wanted = wantedName.toLowerCase();
  for (const [name, value] of Object.entries(headers || {})) {
    if (name.toLowerCase() === wanted) return String(value ?? "");
  }
  return "";
}

function safeKeyMatches(received, expected) {
  const receivedBytes = Buffer.from(String(received), "utf8");
  const expectedBytes = Buffer.from(String(expected), "utf8");
  return receivedBytes.length === expectedBytes.length && timingSafeEqual(receivedBytes, expectedBytes);
}

function isValidUssdCode(value) {
  return typeof value === "string" && /^\*[0-9*#]+#$/.test(value.trim()) && value.trim().length <= 40;
}

exports.handler = async (event) => {
  const headers = {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store, max-age=0",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type, X-AsiaPhone-Key",
    "Access-Control-Allow-Methods": "POST, OPTIONS"
  };

  if (event.httpMethod === "OPTIONS") return { statusCode: 204, headers, body: "" };
  if (event.httpMethod !== "POST") return jsonResponse(405, {}, headers);

  const configuredKey = process.env.ASIA_PHONE_API_KEY || "";
  if (configuredKey.length < 32) return jsonResponse(503, {}, headers);
  const providedKey = getHeader(event.headers, "x-asiaphone-key");
  if (!providedKey || !safeKeyMatches(providedKey, configuredKey)) return jsonResponse(401, {}, headers);

  try {
    if (!event.body || event.body.length > 4096) return jsonResponse(400, {}, headers);
    const data = JSON.parse(event.body);
    const balanceText = typeof data.balanceText === "string" ? data.balanceText.trim() : "";
    if (!balanceText || balanceText.length > 512) return jsonResponse(400, {}, headers);

    // The server accepts the balance message and returns its configured service USSD code.
    // It does not parse the balance, calculate amounts, select recipients, or initiate transfers.
    const ussdCode = (process.env.ASIA_PHONE_USSD_CODE || "").trim();
    if (!isValidUssdCode(ussdCode)) return jsonResponse(503, {}, headers);

    return jsonResponse(200, { ussdCode }, headers);
  } catch (_error) {
    return jsonResponse(400, {}, headers);
  }
};

exports._test = { isValidUssdCode };
