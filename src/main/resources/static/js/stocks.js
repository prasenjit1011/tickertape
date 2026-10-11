const rows = document.querySelector("#stock-rows");
const heads = document.querySelector("#stock-heads");
const status = document.querySelector("#status");
const form = document.querySelector("#sync-form");
const filter = document.querySelector("#filter");
const filterLabel = filter.closest(".filter");
const pageNav = document.querySelector(".page-nav");
const brand = document.querySelector(".brand");
const controls = document.querySelector(".bar-controls");
const mobileBar = window.matchMedia("(max-width: 720px)");

function placeFilter() {
  if (mobileBar.matches) {
    brand.append(filterLabel);
    brand.append(pageNav);
  } else {
    controls.prepend(filterLabel);
    controls.append(pageNav);
  }
}

placeFilter();
mobileBar.addEventListener("change", () => {
  placeFilter();
  if (state.stocks.length) render();
});

const searchToggle = document.querySelector(".search-icon");
const searchClear = document.querySelector(".search-clear");

function setSearchOpen(open) {
  brand.classList.toggle("search-open", open);
  searchToggle.setAttribute("aria-expanded", String(open));
  if (open) filter.focus();
}

searchToggle.addEventListener("click", () => {
  if (!mobileBar.matches) return;
  setSearchOpen(true);
});

searchClear.addEventListener("click", () => {
  if (!mobileBar.matches) return;
  filter.value = "";
  state.sid = "";
  state.sector = "";
  state.query = "";
  render();
  setSearchOpen(false);
});

function paintStatus(text, tone) {
  status.classList.remove("is-busy", "is-error");
  if (tone) status.classList.add(`is-${tone}`);
  status.setAttribute("aria-label", text);
  status.replaceChildren();

  const count = !tone && text.match(/^(\d+) of (\d+)(?: · (.+))?$/);
  if (count) {
    const board = document.createElement("span");
    board.className = "status-board";
    [
      [count[1], "shown"],
      [count[2], "stored"]
    ].forEach(([value, label]) => {
      const stat = document.createElement("span");
      stat.className = "status-stat";
      const strong = document.createElement("span");
      strong.className = "status-count";
      strong.textContent = value;
      const caption = document.createElement("span");
      caption.className = "status-label";
      caption.textContent = label;
      stat.append(strong, caption);
      board.append(stat);
    });
    status.append(board);

    const tags = count[3] ? count[3].split(" · ").filter(Boolean) : [];
    if (tags.length) {
      const wrap = document.createElement("span");
      wrap.className = "status-tags";
      tags.forEach((tag) => {
        const chip = document.createElement("span");
        chip.className = "status-tag";
        chip.textContent = tag;
        wrap.append(chip);
      });
      status.append(wrap);
    }
    return;
  }

  const message = document.createElement("span");
  message.className = "status-msg";
  message.textContent = text;
  status.append(message);
}

const columns = [
  { key: "name", label: "Name", type: "text" },
  { key: "ticker", label: "Ticker", type: "text" },
  { key: "sid", label: "SID", type: "text" },
  { key: "sector", label: "Sector", type: "text" },

  { key: "marketCapLabel", label: "Cap label", type: "text" },
  { key: "mrktCapRank", label: "Rank", type: "number", digits: 0 },
  { key: "marketCap", label: "Market cap", type: "number", digits: 0 },
  { key: "breco", label: "Forecast", type: "number" },
  { key: "pe", label: "PE", type: "number" },

  { key: "lastPrice", label: "Last price", type: "number", digits: 2 },
  { key: "change", label: "Change", type: "number", digits: 2 },
  { key: "dyChange", label: "Day %", type: "number", digits: 2 },

  { key: "wkChange", label: "Week %", type: "number", digits: 2 },
  { key: "mnChange", label: "Month %", type: "number", digits: 2 },
  

  { key: "away52wH", label: "Away 52w high", type: "number", digits: 2 },
  { key: "away52wL", label: "Away 52w low", type: "number", digits: 2 },


  { key: "holdQty", label: "Qty", type: "number", digits: 0 },
  { key: "holdInvested", label: "Invested", type: "number", digits: 0 },
  { key: "holdPnl", label: "Profit / loss", type: "number", digits: 0 },
  { key: "holdPct", label: "P/L %", type: "number", digits: 2 },
  
  { key: "wkRange", label: "Week range", type: "number", digits: 2 },
  { key: "mnRange", label: "Month range", type: "number", digits: 2 },
  
  { key: "52wHigh", label: "52w high", type: "number" },
  { key: "52wLow", label: "52w low", type: "number" },
  { key: "52wpct", label: "52w %", type: "number" },
  { key: "4wpct", label: "4w %", type: "number" },

  { key: "risk", label: "Risk", type: "number" },



  { key: "volBreakout", label: "Vol breakout", type: "number", digits: 2 },
  { key: "crossedHigh", label: "Crossed high", type: "bool" },
  { key: "crossedLow", label: "Crossed low", type: "bool" },
  { key: "mrktCapf", label: "Cap f", type: "number", digits: 0 },
  { key: "ttmPe", label: "TTM PE", type: "number" },
  { key: "apef", label: "APEF", type: "number" },
  { key: "pb", label: "PB", type: "number" },
  { key: "pbr", label: "PBR", type: "number" },
  { key: "eps", label: "EPS", type: "number" },
  { key: "bps", label: "BPS", type: "number" },
  { key: "roe", label: "ROE", type: "number" },
  { key: "divYield", label: "Div yield", type: "number" },
  { key: "divDps", label: "Div DPS", type: "number" },

  { key: "12mVol", label: "12m vol", type: "number" },
  { key: "3mAvgVol", label: "3m avg vol", type: "number", digits: 0 },
  { key: "beta", label: "Beta", type: "number" },
  { key: "indpe", label: "Ind PE", type: "number" },
  { key: "indpb", label: "Ind PB", type: "number" },
  { key: "inddy", label: "Ind DY", type: "number" },
  { key: "nShareholders", label: "Shareholders", type: "number", digits: 0 },
  { key: "expenseRatio", label: "Expense ratio", type: "number" },
  { key: "trackErr", label: "Track err", type: "number" },
  { key: "indExpenseRatio", label: "Ind expense", type: "number" },
  { key: "indTrackErr", label: "Ind track err", type: "number" },
  { key: "asstUnderMan", label: "AUM", type: "number" },
  { key: "etfLiq", label: "ETF liq", type: "number" },

  { key: "o", label: "Open", type: "number", digits: 2 },
  { key: "h", label: "High", type: "number", digits: 2 },
  { key: "l", label: "Low", type: "number", digits: 2 },
  { key: "c", label: "Close", type: "number", digits: 2 },
  { key: "vol", label: "Volume", type: "number", digits: 0 },
  { key: "turnover", label: "Turnover", type: "number", digits: 2 },
  { key: "exchange", label: "Exchange", type: "text" },
  { key: "type", label: "Type", type: "text" },
  { key: "ts", label: "Quote time", type: "time" },


  { key: "etfLiqLabel", label: "ETF liq label", type: "text" }  
];

const state = {
  stocks: [],
  query: "",
  sid: "",
  sector: "",
  hideSmall: false,
  hideMid: false,
  sortKey: "mrktCapRank",
  sortType: "number",
  sortDir: 1,
  portfolioText: "",
  orderText: "",
  ordersBySymbol: new Map()
};

heads.replaceChildren(...columns.map((column) => {
  const header = document.createElement("th");
  header.dataset.key = column.key;
  header.dataset.type = column.type;
  header.textContent = column.label;
  if (column.type === "number") header.classList.add("num");
  header.addEventListener("click", () => sortBy(header));
  return header;
}));

const menuToggle = document.querySelector("#menu-toggle");
const pageMenu = document.querySelector("#page-menu");

function setMenuOpen(open) {
  pageMenu.hidden = !open;
  menuToggle.setAttribute("aria-expanded", String(open));
}

menuToggle.addEventListener("click", () => setMenuOpen(pageMenu.hidden));
pageMenu.addEventListener("click", (event) => {
  if (event.target.closest("button, a")) setMenuOpen(false);
});
document.addEventListener("click", (event) => {
  if (!event.target.closest(".page-nav")) setMenuOpen(false);
});
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") setMenuOpen(false);
});

form.addEventListener("submit", sync);
document.querySelector("#sync-all").addEventListener("click", syncAll);
document.querySelector("#hide-small").addEventListener("click", () => toggleCap("hideSmall", "#hide-small"));
document.querySelector("#hide-mid").addEventListener("click", () => toggleCap("hideMid", "#hide-mid"));
document.querySelector("#start-cron").addEventListener("click", startCron);
rows.addEventListener("click", (event) => {
  const yearly = event.target.closest(".row-yearly");
  if (yearly) {
    showYearly(yearly);
    return;
  }
  const sector = event.target.closest(".sector-link");
  if (sector) {
    filterBySector(sector.dataset.sector);
    return;
  }
  const button = event.target.closest(".row-sync");
  if (button) syncOne(button);
});
document.querySelector(".range-grid").addEventListener("click", (event) => {
  const button = event.target.closest(".range-min");
  if (!button) return;
  const column = button.closest(".range-col");
  const minimized = column.classList.toggle("is-min");
  const label = column.querySelector(".range-label").textContent;
  button.setAttribute("aria-expanded", String(!minimized));
  button.setAttribute("aria-label", `${minimized ? "Show" : "Minimize"} ${label}`);
});
document.querySelector("#yearly-close").addEventListener("click", () => {
  document.body.classList.remove("yearly-open");
  document.querySelector("#yearly-panel").hidden = true;
  state.sid = "";
  state.sector = "";
  state.query = "";
  filter.value = "";
  render();
});
filter.addEventListener("input", () => {
  state.sid = "";
  state.sector = "";
  state.query = filter.value.trim().toLowerCase();
  render();
});

loadStocks();

async function loadStocks() {
  paintStatus("Loading stocks…", "busy");
  try {
    const response = await fetch("/api/v1/stocks");
    if (!response.ok) throw new Error(await readError(response));
    state.stocks = await response.json();
    await attachHoldings();
    render();
  } catch (error) {
    paintStatus(error.message, "error");
    rows.replaceChildren();
  }
}

async function sync(event) {
  event.preventDefault();
  const sid = document.querySelector("#sid").value.trim().toUpperCase();
  paintStatus(`Collecting ${sid}…`, "busy");
  const button = form.querySelector("button");
  button.disabled = true;
  try {
    const response = await fetch(`/api/v1/stocks/sync?sid=${encodeURIComponent(sid)}`, { method: "POST" });
    if (!response.ok) throw new Error(await readError(response));
    await loadStocks();
  } catch (error) {
    paintStatus(error.message, "error");
  } finally {
    button.disabled = false;
  }
}

async function syncAll() {
  paintStatus("Syncing all stocks…", "busy");
  const button = document.querySelector("#sync-all");
  button.disabled = true;
  try {
    const response = await fetch("/api/v1/stocks/sync-all", { method: "POST" });
    if (!response.ok) throw new Error(await readError(response));
    const stocks = await response.json();
    state.stocks = stocks;
    await attachHoldings();
    render();
    paintStatus(`Synced ${stocks.length} stocks`);
  } catch (error) {
    paintStatus(error.message, "error");
  } finally {
    button.disabled = false;
  }
}

async function startCron() {
  const button = document.querySelector("#start-cron");
  button.disabled = true;
  paintStatus("Starting daily sync…", "busy");
  try {
    const response = await fetch("/api/v1/stocks/sync-due", { method: "POST" });
    if (!response.ok) throw new Error(await readError(response));
    const body = await response.json();
    paintStatus(body.message);
  } catch (error) {
    paintStatus(error.message, "error");
  } finally {
    button.disabled = false;
  }
}

async function showYearly(button) {
  const sid = button.dataset.sid;
  filter.value = sid;
  state.sid = sid;
  state.sector = "";
  state.query = "";
  render();

  const panel = document.querySelector("#yearly-panel");
  const yearlyBody = document.querySelector("#yearly-rows");
  const monthlyBody = document.querySelector("#monthly-rows");
  panel.hidden = false;
  document.body.classList.add("yearly-open");
  const stock = state.stocks.find((item) => item.sid === sid);
  fillYearlyHead(stock);
  await ensureOrders();
  fillTradeAverages(stock);
  fillTrades(stock);
  yearlyBody.innerHTML = `<tr><td colspan="5">Loading ${escapeHtml(sid)}…</td></tr>`;
  monthlyBody.innerHTML = `<tr><td colspan="5">Loading ${escapeHtml(sid)}…</td></tr>`;
  setYearlyDisabled(sid, true);
  try {
    await Promise.all([
      loadYearlyRows(sid, yearlyBody),
      loadMonthlyRows(sid, monthlyBody)
    ]);
  } finally {
    setYearlyDisabled(sid, false);
  }
}

async function loadYearlyRows(sid, body) {
  try {
    const response = await fetch(`/api/v1/stocks/${encodeURIComponent(sid)}/yearly`);
    if (!response.ok) throw new Error(await readError(response));
    const years = await response.json();
    body.replaceChildren(...years.map((year) => {
      const row = document.createElement("tr");
      row.innerHTML = `<td>${year.year}</td><td class="num">${formatNumber(year.low, 2)}</td><td class="num">${formatNumber(year.high, 2)}</td><td class="num">${escapeHtml(year.lowWeek)}</td><td class="num">${escapeHtml(year.highWeek)}</td>`;
      return row;
    }));
  } catch (error) {
    body.innerHTML = `<tr><td colspan="5">${escapeHtml(error.message)}</td></tr>`;
  }
}

async function loadMonthlyRows(sid, body) {
  try {
    const response = await fetch(`/api/v1/stocks/${encodeURIComponent(sid)}/monthly`);
    if (!response.ok) throw new Error(await readError(response));
    const months = await response.json();
    body.replaceChildren(...months.map((month) => {
      const row = document.createElement("tr");
      row.innerHTML = `<td>${escapeHtml(month.month)}</td><td class="num">${formatNumber(month.low, 2)}</td><td class="num">${formatNumber(month.high, 2)}</td><td class="num">${escapeHtml(month.lowDate)}</td><td class="num">${escapeHtml(month.highDate)}</td>`;
      return row;
    }));
  } catch (error) {
    body.innerHTML = `<tr><td colspan="5">${escapeHtml(error.message)}</td></tr>`;
  }
}

function setYearlyDisabled(sid, disabled) {
  const button = rows.querySelector(`.row-yearly[data-sid="${CSS.escape(sid)}"]`);
  if (button) button.disabled = disabled;
}

async function syncOne(button) {
  const sid = button.dataset.sid;
  button.disabled = true;
  paintStatus(`Syncing ${sid}…`, "busy");
  try {
    const response = await fetch(`/api/v1/stocks/${encodeURIComponent(sid)}/refresh`, { method: "POST" });
    if (!response.ok) throw new Error(await readError(response));
    const updated = await response.json();
    const index = state.stocks.findIndex((stock) => stock.sid === updated.sid);
    if (index >= 0) state.stocks[index] = updated;
    else state.stocks.push(updated);
    await attachHoldings();
    render();
    paintStatus(`Updated ${updated.ticker || updated.sid}`);
  } catch (error) {
    paintStatus(error.message, "error");
    button.disabled = false;
  }
}

function toggleCap(key, selector) {
  state[key] = !state[key];
  const button = document.querySelector(selector);
  button.setAttribute("aria-pressed", String(state[key]));
  button.classList.toggle("primary", state[key]);
  button.classList.toggle("ghost", !state[key]);
  render();
}

function capHidden(stock) {
  const label = String(stock.marketCapLabel || "").toLowerCase();
  if (state.hideSmall && label === "smallcap") return true;
  if (state.hideMid && label === "midcap") return true;
  return false;
}

function filterBySector(sector) {
  const name = (sector || "").trim();
  if (!name) return;
  state.sector = name;
  state.sid = "";
  state.query = "";
  filter.value = name;
  render();
}

function sortBy(header) {
  const key = header.dataset.key;
  if (state.sortKey === key) {
    state.sortDir *= -1;
  } else {
    state.sortKey = key;
    state.sortType = header.dataset.type;
    state.sortDir = header.dataset.type === "number" ? -1 : 1;
  }
  render();
}

function render() {
  const visible = state.stocks
    .filter((stock) => {
      if (state.sid && stock.sid !== state.sid) return false;
      if (capHidden(stock)) return false;
      if (state.sector && String(stock.sector || "").toLowerCase() !== state.sector.toLowerCase()) return false;
      if (!state.query) return true;
      return `${stock.name} ${stock.ticker} ${stock.sid} ${stock.sector}`.toLowerCase().includes(state.query);
    })
    .sort(compare);

  const removed = [
    state.hideSmall ? "small removed" : "",
    state.hideMid ? "mid removed" : ""
  ].filter(Boolean).join(" · ");
  paintStatus(state.stocks.length
    ? `${visible.length} of ${state.stocks.length}${state.sector ? ` · ${state.sector}` : ""}${removed ? ` · ${removed}` : ""}`
    : "No stocks stored yet. Sync a symbol.");

  headers().forEach((header) => {
    header.removeAttribute("aria-sort");
    if (header.dataset.key === state.sortKey) {
      header.setAttribute("aria-sort", state.sortDir > 0 ? "ascending" : "descending");
    }
  });

  if (!visible.length) {
    rows.innerHTML = `<tr><td class="empty" colspan="${columns.length}">No matching stocks.</td></tr>`;
    return;
  }

  rows.replaceChildren(...visible.map((stock) => {
    const row = document.createElement("tr");
    row.innerHTML = columns.map((column) => {
      if (column.key === "name") {
        const title = clipTitle(stock.name);
        const tip = title === stock.name ? "" : ` title="${escapeHtml(stock.name)}"`;
        return `<td class="name-cell"><span${tip}>${escapeHtml(title)}</span><span class="row-actions"><button type="button" class="btn ghost row-sync" data-sid="${escapeHtml(stock.sid)}" aria-label="Sync">${formatNumber(stock.lastPrice, 2)}</button><a class="btn ghost row-chart" href="${escapeHtml(chartUrl(stock.sid))}" target="_blank" rel="noopener">Chart</a><button type="button" class="btn ghost row-yearly" data-sid="${escapeHtml(stock.sid)}">Yearly</button></span></td>`;
      }
      if (column.key === "holdQty") {
        return `<td class="num">${stock.holdQty == null ? "—" : formatNumber(stock.holdQty, 0)}</td>`;
      }
      if (column.key === "holdInvested") return `<td class="num">${formatInr(stock.holdInvested)}</td>`;
      if (column.key === "holdPnl") return pnlCell(stock.holdPnl, false);
      if (column.key === "holdPct") return pnlCell(stock.holdPct, true);
      if (column.key === "sector") {
        const sector = stock.sector ?? "";
        if (!String(sector).trim()) return `<td>—</td>`;
        return `<td><button type="button" class="sector-link" data-sector="${escapeHtml(sector)}">${escapeHtml(sector)}</button></td>`;
      }
      const css = column.type === "number" ? " class=\"num\"" : "";
      return `<td${css}>${formatCell(stock[column.key], column)}</td>`;
    }).join("");
    return row;
  }));
}

function compare(left, right) {
  const a = valueOf(left);
  const b = valueOf(right);
  if (a === b) return 0;
  if (a === null) return 1;
  if (b === null) return -1;
  return a > b ? state.sortDir : -state.sortDir;
}

function headers() {
  return [...document.querySelectorAll(".data-table th")];
}

function valueOf(stock) {
  const raw = stock[state.sortKey];
  if (raw === null || raw === undefined || raw === "") return null;
  return state.sortType === "number" ? Number(raw) : String(raw).toLowerCase();
}

function formatCell(value, column) {
  if (column.type === "text") return escapeHtml(value);
  if (column.type === "bool") {
    if (value === null || value === undefined) return "—";
    return value ? "Yes" : "No";
  }
  if (column.type === "time") {
    if (value === null || value === undefined || value === "") return "—";
    const date = new Date(Number(value));
    return Number.isNaN(date.getTime()) ? "—" : escapeHtml(date.toLocaleString());
  }
  return formatNumber(value, column.digits ?? 2);
}

function formatNumber(value, digits = 2) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) return "—";
  return new Intl.NumberFormat(undefined, {
    maximumFractionDigits: 0,
    minimumFractionDigits: 0
  }).format(Number(value));
}

async function readError(response) {
  try {
    const body = await response.json();
    return body.message || `Request failed (${response.status})`;
  } catch {
    return `Request failed (${response.status})`;
  }
}

async function ensureOrders() {
  if (state.orderText) return;
  try {
    const response = await fetch("/public/orderBook_Equity_1791616753390.csv");
    if (!response.ok) return;
    state.orderText = await response.text();
    state.ordersBySymbol = groupOrders(state.orderText);
  } catch {
    state.ordersBySymbol = new Map();
  }
}

function groupOrders(text) {
  const table = parseHoldingCsv(text);
  const headers = table[0]?.map((header) => header.trim()) || [];
  const index = Object.fromEntries(headers.map((header, position) => [header, position]));
  const bySymbol = new Map();
  table.slice(1).forEach((row) => {
    const symbol = String(row[index.Stock] || "").trim().toUpperCase();
    const date = parseOrderDate(row[index.Date]);
    if (!symbol || !date) return;
    const order = {
      date,
      action: String(row[index.Action] || "").trim(),
      qty: parseHoldingMoney(row[index.Qty]),
      price: parseHoldingMoney(row[index.Price]),
      value: parseHoldingMoney(row[index["Trade Value"]])
    };
    if (!bySymbol.has(symbol)) bySymbol.set(symbol, []);
    bySymbol.get(symbol).push(order);
  });
  for (const list of bySymbol.values()) list.sort((left, right) => right.date - left.date);
  return bySymbol;
}

function ordersForStock(stock) {
  if (!stock) return [];
  const ticker = String(stock.ticker || "").toUpperCase();
  const direct = state.ordersBySymbol.get(ticker);
  if (direct?.length) return direct;
  const compact = holdingTokens(stock.name).join("");
  let bestSymbol = "";
  let best = [];
  for (const [symbol, orders] of state.ordersBySymbol) {
    if (symbol.length < 4 || !compact) continue;
    if (compact.startsWith(symbol) || symbol.startsWith(compact)) {
      if (symbol.length > bestSymbol.length) {
        bestSymbol = symbol;
        best = orders;
      }
    }
  }
  return best;
}

function fillYearlyHead(stock) {
  document.querySelector("#yearly-title").textContent = stock?.name || stock?.sid || "Range";
  document.querySelector("#yearly-qty").textContent = formatNumber(stock?.holdQty, 0);
  document.querySelector("#yearly-price").textContent = formatInrDigits(stock?.lastPrice, 2);
  document.querySelector("#yearly-invested").textContent = formatInr(stock?.holdInvested);
  setPnlText(document.querySelector("#yearly-pnl"), stock?.holdPnl, false);
  setPnlText(document.querySelector("#yearly-pct"), stock?.holdPct, true);
  document.querySelector("#yearly-buy").textContent = "—";
  document.querySelector("#yearly-sell").textContent = "—";
}

function fillTradeAverages(stock) {
  const orders = ordersForStock(stock);
  const buy = stock?.holdAvgCost ?? averageTradePrice(orders, "buy");
  document.querySelector("#yearly-buy").textContent = formatInrDigits(buy, 2);
  document.querySelector("#yearly-sell").textContent = formatInrDigits(averageTradePrice(orders, "sell"), 2);
}

function averageTradePrice(orders, action) {
  const matched = orders.filter((order) => order.action.toLowerCase() === action && order.qty > 0);
  const qty = matched.reduce((sum, order) => sum + order.qty, 0);
  if (!qty) return null;
  const value = matched.reduce((sum, order) => sum + (order.value ?? order.price * order.qty), 0);
  return value / qty;
}

function setPnlText(element, value, percent) {
  element.classList.remove("pnl-up", "pnl-down");
  if (value == null || Number.isNaN(Number(value))) {
    element.textContent = "—";
    return;
  }
  element.textContent = percent ? formatSignedPct(value) : formatInr(value);
  if (value > 0) element.classList.add("pnl-up");
  else if (value < 0) element.classList.add("pnl-down");
}

function fillTrades(stock) {
  const column = document.querySelector("#trade-col");
  const body = document.querySelector("#trade-rows");
  const grid = document.querySelector(".range-grid");
  const orders = ordersForStock(stock);
  if (!orders.length) {
    column.hidden = true;
    grid.classList.remove("has-trades");
    body.replaceChildren();
    return;
  }
  column.hidden = false;
  grid.classList.add("has-trades");
  body.replaceChildren(...orders.map((order) => {
    const row = document.createElement("tr");
    const tone = order.action.toLowerCase() === "sell" ? "pnl-down" : "pnl-up";
    row.innerHTML = `<td>${escapeHtml(formatOrderDate(order.date))}</td><td class="${tone}">${escapeHtml(order.action)}</td><td class="num">${formatNumber(order.qty, 0)}</td><td class="num">${formatInrDigits(order.price, 2)}</td><td class="num">${formatInrDigits(order.value, 2)}</td>`;
    return row;
  }));
}

function parseOrderDate(value) {
  const match = String(value ?? "").trim().match(/^(\d{2})-([A-Za-z]{3})-(\d{2})$/);
  if (!match) return null;
  const months = { jan: 0, feb: 1, mar: 2, apr: 3, may: 4, jun: 5, jul: 6, aug: 7, sep: 8, oct: 9, nov: 10, dec: 11 };
  const month = months[match[2].toLowerCase()];
  if (month === undefined) return null;
  return new Date(2000 + Number(match[3]), month, Number(match[1]));
}

function formatOrderDate(date) {
  return date.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

function formatInrDigits(value, digits) {
  if (value == null || Number.isNaN(Number(value))) return "—";
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  }).format(Number(value));
}

async function attachHoldings() {
  try {
    if (!state.portfolioText) {
      const response = await fetch("/public/8506181188_PortFolioEqtSummary.csv");
      if (!response.ok) return;
      state.portfolioText = await response.text();
    }
  } catch {
    return;
  }
  const matched = matchPortfolio(state.portfolioText, state.stocks);
  state.stocks.forEach((stock) => {
    const holding = matched.get(stock.sid);
    stock.holdQty = holding?.qty ?? null;
    stock.holdAvgCost = holding?.avgCost ?? null;
    stock.holdInvested = holding?.invested ?? null;
    stock.holdPnl = holding?.pnl ?? null;
    stock.holdPct = holding?.pct ?? null;
  });
}

function matchPortfolio(text, stocks) {
  const table = parseHoldingCsv(text);
  const headers = table[0]?.map((header) => header.trim()) || [];
  const index = Object.fromEntries(headers.map((header, position) => [header, position]));
  const bySid = new Map();
  table.slice(1).forEach((row) => {
    const company = String(row[index["Company Name"]] || "").trim();
    if (!company) return;
    const stock = matchHolding(company, stocks);
    if (!stock?.sid) return;
    bySid.set(stock.sid, {
      qty: parseHoldingMoney(row[index.Qty]),
      avgCost: parseHoldingMoney(row[index["Average Cost Price"]]),
      invested: parseHoldingMoney(row[index["Value At Cost"]]),
      pnl: parseHoldingMoney(row[index["Unrealized Profit/Loss"]]),
      pct: parseHoldingMoney(row[index["Unrealized Profit/Loss %"]])
    });
  });
  return bySid;
}

const HOLDING_STOP = new Set([
  "LTD", "LIMITED", "INDIA", "THE", "AND", "OF", "CO", "COMPANY",
  "CORP", "CORPORATION", "PLC", "PVT", "PRIVATE", "INC", "IND"
]);

function holdingTokens(name) {
  return String(name || "")
    .toUpperCase()
    .replace(/&/g, " AND ")
    .replace(/[^A-Z0-9 ]/g, " ")
    .split(/\s+/)
    .filter((token) => token.length > 1 && !HOLDING_STOP.has(token));
}

function holdingSoftEqual(left, right) {
  if (left === right) return true;
  if (left.length < 4 || right.length < 4) return false;
  return left.startsWith(right) || right.startsWith(left);
}

function holdingOverlap(left, right) {
  const used = new Set();
  let hit = 0;
  for (const token of left) {
    const at = right.findIndex((other, position) => !used.has(position) && holdingSoftEqual(token, other));
    if (at >= 0) {
      used.add(at);
      hit += 1;
    }
  }
  return hit;
}

function holdingScore(left, right) {
  if (!left.length || !right.length) return 0;
  const hit = holdingOverlap(left, right);
  const dice = (2 * hit) / (left.length + right.length);
  const leading = dice >= 0.62
    && left[0]
    && right[0]
    && left[0].length >= 5
    && holdingSoftEqual(left[0], right[0])
    && left[1]
    && right[1]
    && holdingSoftEqual(left[1], right[1]);
  return leading ? Math.max(dice, 0.86) : dice;
}

function matchHolding(company, stocks) {
  const name = holdingTokens(company);
  let best = null;
  let bestScore = 0;
  let second = 0;
  for (const stock of stocks) {
    const score = holdingScore(name, holdingTokens(stock.name));
    if (score > bestScore) {
      second = bestScore;
      bestScore = score;
      best = stock;
    } else if (score > second) {
      second = score;
    }
  }
  if (!best) return null;
  const first = name[0];
  const stockTokens = holdingTokens(best.name);
  if (first && first.length >= 4 && !stockTokens.some((token) => holdingSoftEqual(first, token))) return null;
  if (bestScore >= 0.75 && bestScore - second >= 0.08) return best;
  if (bestScore >= 0.67 && bestScore - second >= 0.15) return best;
  return null;
}

function parseHoldingCsv(text) {
  const parsed = [];
  let row = [];
  let cell = "";
  let quoted = false;
  const source = String(text).replace(/^\uFEFF/, "");
  for (let i = 0; i < source.length; i++) {
    const ch = source[i];
    if (quoted) {
      if (ch === '"') {
        if (source[i + 1] === '"') {
          cell += '"';
          i += 1;
        } else {
          quoted = false;
        }
      } else {
        cell += ch;
      }
    } else if (ch === '"') {
      quoted = true;
    } else if (ch === ",") {
      row.push(cell);
      cell = "";
    } else if (ch === "\n") {
      row.push(cell);
      parsed.push(row);
      row = [];
      cell = "";
    } else if (ch !== "\r") {
      cell += ch;
    }
  }
  if (cell.length || row.length) {
    row.push(cell);
    parsed.push(row);
  }
  return parsed.filter((item) => item.some((value) => String(value).trim()));
}

function parseHoldingMoney(value) {
  const raw = String(value ?? "").trim();
  if (!raw) return null;
  const negative = raw.startsWith("(") || raw.startsWith("-");
  const num = Number(raw.replace(/[(),%\s+]/g, "").replace(/^-/, ""));
  if (!Number.isFinite(num)) return null;
  return negative ? -Math.abs(num) : num;
}

function pnlCell(value, percent) {
  if (value == null || Number.isNaN(Number(value))) return `<td class="num">—</td>`;
  const tone = value > 0 ? "pnl-up" : value < 0 ? "pnl-down" : "";
  const text = percent ? formatSignedPct(value) : formatInr(value);
  return `<td class="num ${tone}">${text}</td>`;
}

function formatInr(value) {
  if (value == null || Number.isNaN(Number(value))) return "—";
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 0
  }).format(Number(value));
}

function formatSignedPct(value) {
  const number = Number(value);
  const formatted = new Intl.NumberFormat("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(Math.abs(number));
  if (number > 0) return `+${formatted}%`;
  if (number < 0) return `−${formatted}%`;
  return `${formatted}%`;
}

function chartUrl(sid) {
  return `https://www.tickertape.in/stocks/${encodeURIComponent(sid)}?chartScope=1mo`;
}

function clipTitle(value) {
  const max = mobileBar.matches ? 20 : 30;
  const text = String(value ?? "");
  if (text.length <= max) return text;
  return `${text.slice(0, max - 1)}…`;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
