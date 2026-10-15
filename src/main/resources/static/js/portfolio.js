const PORTFOLIO_URL = "/api/v1/portfolio/holdings";
const ORDERS_URL = "/api/v1/portfolio/orders";

const STOP = new Set([
  "LTD", "LIMITED", "INDIA", "THE", "AND", "OF", "CO", "COMPANY",
  "CORP", "CORPORATION", "PLC", "PVT", "PRIVATE", "INC", "IND"
]);

const columns = [
  { key: "company", label: "Company", type: "text" },
  { key: "inStocks", label: "Listed", type: "mark" },
  { key: "qty", label: "Qty", type: "number", digits: 0 },
  { key: "costValue", label: "Invested", type: "number", digits: 0 },
  { key: "avgCost", label: "Avg cost", type: "number", digits: 2 },
  { key: "price", label: "Price", type: "number", digits: 2 },
  { key: "marketValue", label: "Value", type: "number", digits: 0 },
  { key: "unrealized", label: "Unrealized", type: "number", digits: 0 },
  { key: "unrealizedPct", label: "Live P/L %", type: "number", digits: 2 },
  { key: "dayPct", label: "Day %", type: "number", digits: 2 },
  { key: "realized", label: "Realized", type: "number", digits: 0 },
  { key: "cap", label: "Cap", type: "text" },
  { key: "sector", label: "Sector", type: "text" },
  { key: "away52w", label: "Away 52w", type: "number", digits: 1 },
  { key: "status", label: "Status", type: "status" },
  { key: "lastTrade", label: "Last trade", type: "text" }
];

const statusRank = { review: 0, loss: 1, new: 2, hold: 3, profit: 4, book: 5 };

const chips = [
  { key: "all", label: "All" },
  { key: "review", label: "Review" },
  { key: "loss", label: "Under water" },
  { key: "profit", label: "In profit" },
  { key: "new", label: "Just bought" },
  { key: "hold", label: "Hold" }
];

const state = {
  holdings: [],
  query: "",
  status: "all",
  sortKey: "unrealizedPct",
  sortDir: 1,
  selected: ""
};

function parseOrderDate(value) {
  const iso = String(value ?? "").trim().match(/^(\d{4})-(\d{2})-(\d{2})/);
  if (!iso) return null;
  return new Date(Number(iso[1]), Number(iso[2]) - 1, Number(iso[3]));
}

function tokens(name) {
  return String(name || "")
    .toUpperCase()
    .replace(/&/g, " AND ")
    .replace(/[^A-Z0-9 ]/g, " ")
    .split(/\s+/)
    .filter((token) => token.length > 1 && !STOP.has(token));
}

function softEqual(left, right) {
  if (left === right) return true;
  if (left.length < 4 || right.length < 4) return false;
  return left.startsWith(right) || right.startsWith(left);
}

function overlap(left, right) {
  const used = new Set();
  let hit = 0;
  for (const token of left) {
    const index = right.findIndex((other, position) => !used.has(position) && softEqual(token, other));
    if (index >= 0) {
      used.add(index);
      hit += 1;
    }
  }
  return hit;
}

function nameScore(holdingTokens, stockTokens) {
  if (!holdingTokens.length || !stockTokens.length) return 0;
  const hit = overlap(holdingTokens, stockTokens);
  const dice = (2 * hit) / (holdingTokens.length + stockTokens.length);
  const leading = dice >= 0.62
    && holdingTokens[0]
    && stockTokens[0]
    && holdingTokens[0].length >= 5
    && softEqual(holdingTokens[0], stockTokens[0])
    && holdingTokens[1]
    && stockTokens[1]
    && softEqual(holdingTokens[1], stockTokens[1]);
  return leading ? Math.max(dice, 0.86) : dice;
}

function matchByTicker(symbol, stocks) {
  const ticker = String(symbol || "").trim().toUpperCase();
  if (!ticker) return null;
  return stocks.find((stock) => String(stock.ticker || "").trim().toUpperCase() === ticker) || null;
}

function matchStock(company, stocks) {
  const holdingTokens = tokens(company);
  let best = null;
  let bestScore = 0;
  let second = 0;
  for (const stock of stocks) {
    const score = nameScore(holdingTokens, tokens(stock.name));
    if (score > bestScore) {
      second = bestScore;
      bestScore = score;
      best = stock;
    } else if (score > second) {
      second = score;
    }
  }
  if (!best) return null;
  const first = holdingTokens[0];
  const stockTokens = tokens(best.name);
  if (first && first.length >= 4 && !stockTokens.some((token) => softEqual(first, token))) return null;
  if (bestScore >= 0.75 && bestScore - second >= 0.08) return best;
  if (bestScore >= 0.67 && bestScore - second >= 0.15) return best;
  return null;
}

function ordersFor(company, stock, bySymbol) {
  if (stock?.ticker) {
    const direct = bySymbol.get(String(stock.ticker).toUpperCase());
    if (direct?.length) return direct;
  }
  const compact = tokens(company).join("");
  let bestSymbol = "";
  let best = [];
  for (const [symbol, list] of bySymbol) {
    if (symbol.length < 4 || !compact) continue;
    if (compact.startsWith(symbol) || symbol.startsWith(compact)) {
      if (symbol.length > bestSymbol.length) {
        bestSymbol = symbol;
        best = list;
      }
    }
  }
  return best;
}

function daysSince(date) {
  const now = new Date();
  const start = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const then = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  return Math.round((start - then) / 86400000);
}

function statusOf(holding) {
  const pct = holding.unrealizedPct;
  if (pct == null || Number.isNaN(Number(pct))) return { key: "hold", label: "Hold" };
  const last = holding.orders[0];
  const recentBuy = last && last.action.toLowerCase() === "buy" && last.date && daysSince(last.date) <= 14;
  const away = holding.stock?.away52wH;
  const nearHigh = away != null && away >= 0 && away <= 5;
  const weak = holding.stock
    && holding.stock.wkChange != null
    && holding.stock.mnChange != null
    && Number(holding.stock.wkChange) < 0
    && Number(holding.stock.mnChange) < 0;

  if (recentBuy && Math.abs(pct) < 15) return { key: "new", label: "Just bought" };
  if (pct > 15 && nearHigh) return { key: "book", label: "Book partial" };
  if (pct > 15) return { key: "profit", label: "In profit" };
  if (pct < -20 && weak) return { key: "review", label: "Review" };
  if (pct < -20) return { key: "loss", label: "Under water" };
  if (pct >= -8 && pct <= 8) return { key: "hold", label: "Hold" };
  if (pct > 0) return { key: "profit", label: "In profit" };
  return { key: "loss", label: "Under water" };
}

function buildHoldings(portfolioRows, orderRows, stocks) {
  const bySymbol = new Map();
  orderRows.forEach((row) => {
    const symbol = String(row.symbol || "").trim().toUpperCase();
    const date = parseOrderDate(row.date);
    if (!symbol || !date) return;
    const order = {
      symbol,
      date,
      action: String(row.action || "").trim(),
      qty: row.qty,
      price: row.price,
      value: row.value,
      charges: row.charges,
      exchange: String(row.exchange || "").trim()
    };
    if (!bySymbol.has(symbol)) bySymbol.set(symbol, []);
    bySymbol.get(symbol).push(order);
  });
  for (const list of bySymbol.values()) {
    list.sort((left, right) => right.date - left.date);
  }

  return portfolioRows.map((row) => {
    const symbol = String(row.symbol || "").trim();
    const company = String(row.company || "").trim();
    if (!symbol || !company) return null;
    const stock = matchByTicker(symbol, stocks) || matchStock(company, stocks);
    const orders = ordersFor(company, stock, bySymbol);
    const qty = row.qty;
    const invested = row.costValue;
    const ltp = stock?.lastPrice ?? null;
    const live = ltp != null && qty != null && invested != null ? Number(ltp) * Number(qty) - Number(invested) : null;
    const livePct = live != null && Number(invested) !== 0 ? (live / Number(invested)) * 100 : null;
    const holding = {
      symbol,
      company: stock?.name || company,
      isin: String(row.isin || "").trim(),
      qty,
      avgCost: row.avgCost,
      price: ltp ?? row.price,
      dayPct: row.dayPct,
      costValue: invested,
      marketValue: ltp != null && qty != null ? Number(ltp) * Number(qty) : row.marketValue,
      realized: row.realized,
      unrealized: live,
      unrealizedPct: livePct,
      stock,
      orders
    };
    holding.status = statusOf(holding);
    return holding;
  }).filter(Boolean);
}

function formatInr(value, digits = 0) {
  if (value == null || Number.isNaN(Number(value))) return "—";
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  }).format(Number(value));
}

function formatSigned(value, digits = 2) {
  if (value == null || Number.isNaN(Number(value))) return "—";
  const number = Number(value);
  const formatted = new Intl.NumberFormat("en-IN", {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  }).format(Math.abs(number));
  if (number > 0) return `+${formatted}`;
  if (number < 0) return `−${formatted}`;
  return formatted;
}

function formatPct(value, digits = 2) {
  const text = formatSigned(value, digits);
  return text === "—" ? text : `${text}%`;
}

function formatPlain(value, digits = 2) {
  if (value == null || Number.isNaN(Number(value))) return "—";
  return new Intl.NumberFormat("en-IN", {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  }).format(Number(value));
}

function formatWhen(date) {
  if (!date) return "—";
  return date.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

function pnlClass(value) {
  if (value == null || Number(value) === 0) return "";
  return Number(value) > 0 ? "pnl-up" : "pnl-down";
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function chartUrl(sid) {
  return `https://www.tickertape.in/stocks/${encodeURIComponent(sid)}?chartScope=1mo`;
}

function passesFilter(holding) {
  if (state.status === "profit") return holding.status.key === "profit" || holding.status.key === "book";
  if (state.status !== "all" && holding.status.key !== state.status) return false;
  if (!state.query) return true;
  const stock = holding.stock;
  const haystack = `${holding.company} ${holding.symbol} ${stock?.ticker || ""} ${stock?.sector || ""}`.toLowerCase();
  return haystack.includes(state.query);
}

function sortValue(holding) {
  const stock = holding.stock;
  switch (state.sortKey) {
    case "company": return holding.company.toLowerCase();
    case "inStocks": return holding.stock ? 0 : 1;
    case "qty": return holding.qty;
    case "avgCost": return holding.avgCost;
    case "price": return holding.price;
    case "marketValue": return holding.marketValue;
    case "unrealized": return holding.unrealized;
    case "unrealizedPct": return holding.unrealizedPct;
    case "dayPct": return holding.dayPct;
    case "realized": return holding.realized;
    case "cap": return stock?.marketCapLabel ? stock.marketCapLabel.toLowerCase() : null;
    case "sector": return stock?.sector ? stock.sector.toLowerCase() : null;
    case "away52w": return stock?.away52wH ?? null;
    case "status": return statusRank[holding.status.key] ?? 9;
    case "lastTrade": return holding.orders[0]?.date?.getTime() ?? null;
    default: return null;
  }
}

function compare(left, right) {
  const a = sortValue(left);
  const b = sortValue(right);
  if (a === b) return left.company.localeCompare(right.company);
  if (a == null) return 1;
  if (b == null) return -1;
  return a > b ? state.sortDir : -state.sortDir;
}

function paintSummary(list) {
  const cost = list.reduce((sum, holding) => sum + (holding.costValue || 0), 0);
  const market = list.reduce((sum, holding) => sum + (holding.marketValue || 0), 0);
  const unreal = list.reduce((sum, holding) => sum + (holding.unrealized || 0), 0);
  const real = list.reduce((sum, holding) => sum + (holding.realized || 0), 0);
  const unrealPct = cost ? (unreal / cost) * 100 : null;
  document.querySelector("#sum-cost").textContent = formatInr(cost);
  document.querySelector("#sum-market").textContent = formatInr(market);
  document.querySelector("#sum-unreal").textContent = formatInr(unreal);
  document.querySelector("#sum-unreal-pct").textContent = unrealPct == null ? "—" : `${formatSigned(unrealPct, 2)}%`;
  document.querySelector("#sum-real").textContent = formatInr(real);
  document.querySelector("#card-unreal").classList.toggle("up", unreal > 0);
  document.querySelector("#card-unreal").classList.toggle("down", unreal < 0);
  document.querySelector("#card-real").classList.toggle("up", real > 0);
  document.querySelector("#card-real").classList.toggle("down", real < 0);
}

function paintChips() {
  const box = document.querySelector("#chips");
  box.replaceChildren(...chips.map((chip) => {
    const count = chip.key === "all"
      ? state.holdings.length
      : chip.key === "profit"
        ? state.holdings.filter((holding) => holding.status.key === "profit" || holding.status.key === "book").length
        : state.holdings.filter((holding) => holding.status.key === chip.key).length;
    const button = document.createElement("button");
    button.type = "button";
    button.className = "chip";
    button.dataset.status = chip.key;
    button.setAttribute("aria-pressed", String(state.status === chip.key));
    button.textContent = `${chip.label} ${count}`;
    button.addEventListener("click", () => {
      state.status = chip.key;
      render();
    });
    return button;
  }));
}

function showDetail(holding) {
  const panel = document.querySelector("#detail");
  panel.hidden = false;
  document.querySelector("#detail-title").textContent = holding.company;
  const ticker = holding.stock?.ticker ? ` · ${holding.stock.ticker}` : "";
  document.querySelector("#detail-meta").textContent = `${holding.symbol}${ticker} · ${formatPlain(holding.qty, 0)} shares · avg ${formatInr(holding.avgCost, 2)}`;
  const stock = holding.stock;
  const metrics = [
    ["Status", holding.status.label],
    ["Unrealized", formatPct(holding.unrealizedPct, 2)],
    ["Realized", formatInr(holding.realized)],
    ["Cap", stock?.marketCapLabel || "—"],
    ["Sector", stock?.sector || "—"],
    ["PE", stock ? formatPlain(stock.pe, 1) : "—"],
    ["Week %", stock ? formatPct(stock.wkChange, 2) : "—"],
    ["Month %", stock ? formatPct(stock.mnChange, 2) : "—"],
    ["Away 52w high", stock && stock.away52wH != null ? `${formatPlain(stock.away52wH, 1)}%` : "—"]
  ];
  const metricRow = document.querySelector("#detail-metrics");
  metricRow.replaceChildren(...metrics.map(([label, value]) => {
    const item = document.createElement("div");
    item.className = "metric";
    item.innerHTML = `<span>${escapeHtml(label)}</span><strong>${escapeHtml(value)}</strong>`;
    return item;
  }));
  if (stock?.sid) {
    const link = document.createElement("a");
    link.className = "metric linkish";
    link.href = chartUrl(stock.sid);
    link.target = "_blank";
    link.rel = "noopener";
    link.innerHTML = "<span>Chart</span><strong>Open</strong>";
    metricRow.append(link);
  }
  const body = document.querySelector("#trade-rows");
  if (!holding.orders.length) {
    body.innerHTML = `<tr><td colspan="7">No matching trades in the order book.</td></tr>`;
    return;
  }
  body.replaceChildren(...holding.orders.map((order) => {
    const row = document.createElement("tr");
    const actionClass = order.action.toLowerCase() === "sell" ? "action-sell" : "action-buy";
    row.innerHTML = `<td>${escapeHtml(formatWhen(order.date))}</td><td class="${actionClass}">${escapeHtml(order.action)}</td><td class="num">${formatPlain(order.qty, 0)}</td><td class="num">${formatInr(order.price, 2)}</td><td class="num">${formatInr(order.value, 2)}</td><td class="num">${formatInr(order.charges, 2)}</td><td>${escapeHtml(order.exchange)}</td>`;
    return row;
  }));
}

function hideDetail() {
  state.selected = "";
  document.querySelector("#detail").hidden = true;
  document.querySelectorAll("#hold-rows tr.is-selected").forEach((row) => row.classList.remove("is-selected"));
}

function render() {
  paintChips();
  const visible = state.holdings.filter(passesFilter).sort(compare);
  const linked = state.holdings.filter((holding) => holding.stock).length;
  document.querySelector("#status").textContent = `${visible.length} of ${state.holdings.length} · ${linked} linked to the stock table`;

  document.querySelectorAll("#hold-heads th").forEach((header) => {
    header.removeAttribute("aria-sort");
    if (header.dataset.key === state.sortKey) {
      header.setAttribute("aria-sort", state.sortDir > 0 ? "ascending" : "descending");
    }
  });

  const body = document.querySelector("#hold-rows");
  if (!visible.length) {
    body.innerHTML = `<tr><td class="empty" colspan="${columns.length}">No matching holdings.</td></tr>`;
    return;
  }

  body.replaceChildren(...visible.map((holding) => {
    const row = document.createElement("tr");
    if (holding.symbol === state.selected) row.classList.add("is-selected");
    const stock = holding.stock;
    const last = holding.orders[0];
    const lastLabel = last ? `${last.action} · ${formatWhen(last.date)}` : "—";
    const booked = holding.realized ? `<span class="pill booked">Booked</span>` : "";
    const ticker = stock?.ticker ? ` · ${escapeHtml(stock.ticker)}` : "";
    const listed = stock
      ? `<span class="mark-yes" role="img" aria-label="RSI, in the stock table"><svg viewBox="0 0 16 16" aria-hidden="true" focusable="false"><path d="M3.5 8.5 6.5 11.5 12.5 4.5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg><span>RSI</span></span>`
      : `<span class="mark-no" role="img" aria-label="CSI, not in the stock table"><svg viewBox="0 0 16 16" aria-hidden="true" focusable="false"><path d="M4.5 4.5 11.5 11.5M11.5 4.5 4.5 11.5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg><span>CSI</span></span>`;
    row.innerHTML = `
      <td><span class="hold-name"><strong>${escapeHtml(holding.company)}</strong><small>${escapeHtml(holding.symbol)}${ticker}</small></span></td>
      <td class="in-stock">${listed}</td>
      <td class="num">${formatPlain(holding.qty, 0)}</td>
      <td class="num">${formatInr(holding.costValue)}</td>
      <td class="num">${formatInr(holding.avgCost, 2)}</td>
      <td class="num">${formatInr(holding.price, 2)}</td>
      <td class="num">${formatInr(holding.marketValue)}</td>
      <td class="num ${pnlClass(holding.unrealized)}">${formatInr(holding.unrealized)}</td>
      <td class="num ${pnlClass(holding.unrealizedPct)}">${formatPct(holding.unrealizedPct, 2)}</td>
      <td class="num ${pnlClass(holding.dayPct)}">${formatPct(holding.dayPct, 2)}</td>
      <td class="num ${pnlClass(holding.realized)}">${formatInr(holding.realized)}</td>
      <td>${escapeHtml(stock?.marketCapLabel || "—")}</td>
      <td>${escapeHtml(stock?.sector || "—")}</td>
      <td class="num">${stock && stock.away52wH != null ? `${formatPlain(stock.away52wH, 1)}%` : "—"}</td>
      <td><span class="pills"><span class="pill ${holding.status.key}">${escapeHtml(holding.status.label)}</span>${booked}</span></td>
      <td>${escapeHtml(lastLabel)}</td>`;
    row.addEventListener("click", () => {
      if (state.selected === holding.symbol) {
        hideDetail();
        return;
      }
      state.selected = holding.symbol;
      render();
      showDetail(holding);
    });
    return row;
  }));

  if (state.selected) {
    const selected = visible.find((holding) => holding.symbol === state.selected);
    if (selected) showDetail(selected);
    else hideDetail();
  }
}

function sortBy(header) {
  const key = header.dataset.key;
  if (state.sortKey === key) {
    state.sortDir *= -1;
  } else {
    state.sortKey = key;
    state.sortDir = 1;
  }
  render();
}

async function load() {
  const status = document.querySelector("#status");
  status.textContent = "Loading portfolio…";
  try {
    const [portfolioRes, orderRes, stockRes] = await Promise.all([
      fetch(PORTFOLIO_URL),
      fetch(ORDERS_URL),
      fetch("/api/v1/stocks")
    ]);
    if (!portfolioRes.ok || !orderRes.ok || !stockRes.ok) {
      throw new Error("Could not load portfolio, orders, or stocks");
    }
    const [portfolioRows, orderRows, stocks] = await Promise.all([
      portfolioRes.json(),
      orderRes.json(),
      stockRes.json()
    ]);
    state.holdings = buildHoldings(portfolioRows, orderRows, stocks);
    paintSummary(state.holdings);
    render();
  } catch (error) {
    status.textContent = error.message;
  }
}

if (typeof document !== "undefined" && document.querySelector("#hold-rows")) {
  const heads = document.querySelector("#hold-heads");
  heads.replaceChildren(...columns.map((column) => {
    const header = document.createElement("th");
    header.dataset.key = column.key;
    header.dataset.type = column.type;
    header.textContent = column.label;
    if (column.type === "number") header.classList.add("num");
    if (column.type === "mark") header.classList.add("in-stock");
    header.addEventListener("click", () => sortBy(header));
    return header;
  }));
  document.querySelector("#filter").addEventListener("input", (event) => {
    state.query = event.target.value.trim().toLowerCase();
    render();
  });
  document.querySelector("#detail-close").addEventListener("click", hideDetail);
  load();
}
