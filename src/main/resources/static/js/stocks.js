const rows = document.querySelector("#stock-rows");
const heads = document.querySelector("#stock-heads");
const status = document.querySelector("#status");
const form = document.querySelector("#sync-form");
const filter = document.querySelector("#filter");

const columns = [
  { key: "name", label: "Name", type: "text" },
  { key: "ticker", label: "Ticker", type: "text" },
  { key: "sid", label: "SID", type: "text" },
  { key: "sector", label: "Sector", type: "text" },

  { key: "marketCapLabel", label: "Cap label", type: "text" },
  { key: "mrktCapRank", label: "Cap rank", type: "number", digits: 0 },
  { key: "marketCap", label: "Market cap", type: "number", digits: 0 },
  { key: "breco", label: "Breco", type: "number" },
  { key: "pe", label: "PE", type: "number" },



  { key: "lastPrice", label: "Last price", type: "number", digits: 2 },
  { key: "change", label: "Change", type: "number", digits: 2 },
  { key: "dyChange", label: "Day %", type: "number", digits: 2 },

  { key: "wkChange", label: "Week %", type: "number", digits: 2 },
  { key: "mnChange", label: "Month %", type: "number", digits: 2 },
  { key: "wkRange", label: "Week range", type: "number", digits: 2 },
  { key: "mnRange", label: "Month range", type: "number", digits: 2 },
  { key: "away52wH", label: "Away 52w high", type: "number", digits: 2 },
  { key: "away52wL", label: "Away 52w low", type: "number", digits: 2 },

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
  sortKey: "mrktCapRank",
  sortType: "number",
  sortDir: 1
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

form.addEventListener("submit", sync);
document.querySelector("#sync-all").addEventListener("click", syncAll);
rows.addEventListener("click", (event) => {
  const button = event.target.closest(".row-sync");
  if (button) syncOne(button);
});
filter.addEventListener("input", () => {
  state.query = filter.value.trim().toLowerCase();
  render();
});

loadStocks();

async function loadStocks() {
  status.textContent = "Loading stocks…";
  try {
    const response = await fetch("/api/v1/stocks");
    if (!response.ok) throw new Error(await readError(response));
    state.stocks = await response.json();
    render();
  } catch (error) {
    status.textContent = error.message;
    rows.replaceChildren();
  }
}

async function sync(event) {
  event.preventDefault();
  const sid = document.querySelector("#sid").value.trim().toUpperCase();
  status.textContent = `Collecting ${sid}…`;
  const button = form.querySelector("button");
  button.disabled = true;
  try {
    const response = await fetch(`/api/v1/stocks/sync?sid=${encodeURIComponent(sid)}`, { method: "POST" });
    if (!response.ok) throw new Error(await readError(response));
    await loadStocks();
  } catch (error) {
    status.textContent = error.message;
  } finally {
    button.disabled = false;
  }
}

async function syncAll() {
  status.textContent = "Syncing all stocks…";
  const button = document.querySelector("#sync-all");
  button.disabled = true;
  try {
    const response = await fetch("/api/v1/stocks/sync-all", { method: "POST" });
    if (!response.ok) throw new Error(await readError(response));
    const stocks = await response.json();
    state.stocks = stocks;
    render();
    status.textContent = `Synced ${stocks.length} stocks`;
  } catch (error) {
    status.textContent = error.message;
  } finally {
    button.disabled = false;
  }
}

async function syncOne(button) {
  const sid = button.dataset.sid;
  button.disabled = true;
  status.textContent = `Syncing ${sid}…`;
  try {
    const response = await fetch(`/api/v1/stocks/${encodeURIComponent(sid)}/refresh`, { method: "POST" });
    if (!response.ok) throw new Error(await readError(response));
    const updated = await response.json();
    const index = state.stocks.findIndex((stock) => stock.sid === updated.sid);
    if (index >= 0) state.stocks[index] = updated;
    else state.stocks.push(updated);
    render();
    status.textContent = `Updated ${updated.ticker || updated.sid}`;
  } catch (error) {
    status.textContent = error.message;
    button.disabled = false;
  }
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
      if (!state.query) return true;
      return `${stock.name} ${stock.ticker} ${stock.sid} ${stock.sector}`.toLowerCase().includes(state.query);
    })
    .sort(compare);

  status.textContent = state.stocks.length
    ? `${visible.length} of ${state.stocks.length}`
    : "No stocks stored yet. Sync a symbol.";

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
        return `<td class="name-cell"><span>${escapeHtml(stock.name)}</span><button type="button" class="btn ghost row-sync" data-sid="${escapeHtml(stock.sid)}">Sync</button></td>`;
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
    maximumFractionDigits: digits,
    minimumFractionDigits: digits
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

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
