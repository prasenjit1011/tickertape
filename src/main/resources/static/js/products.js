const api = "/api/v1/products";
const pageSize = 10;

const state = {
  page: 0,
  totalPages: 1,
  products: [],
  query: "",
  pendingDeleteId: null
};

const rows = document.querySelector("#product-rows");
const status = document.querySelector("#status");
const form = document.querySelector("#product-form");
const formError = document.querySelector("#form-error");
const formTitle = document.querySelector("#form-title");
const cancelEdit = document.querySelector("#cancel-edit");
const pageLabel = document.querySelector("#page-label");
const dialog = document.querySelector("#delete-dialog");

document.querySelector("#new-product").addEventListener("click", resetForm);
cancelEdit.addEventListener("click", resetForm);
form.addEventListener("submit", saveProduct);
document.querySelector("#prev-page").addEventListener("click", () => changePage(-1));
document.querySelector("#next-page").addEventListener("click", () => changePage(1));
document.querySelector("#search").addEventListener("input", (event) => {
  state.query = event.target.value.trim().toLowerCase();
  renderRows();
});
document.querySelector("#confirm-delete").addEventListener("click", confirmDelete);

loadProducts();

async function loadProducts() {
  status.textContent = "Loading products…";
  try {
    const response = await fetch(`${api}?page=${state.page}&size=${pageSize}&sort=createdAt,desc`);
    if (!response.ok) throw new Error(await readError(response));
    const body = await response.json();
    state.products = body.content ?? [];
    state.totalPages = Math.max(body.totalPages ?? 1, 1);
    status.textContent = state.products.length ? "" : "No products yet.";
    renderRows();
    pageLabel.textContent = `Page ${state.page + 1} of ${state.totalPages}`;
    document.querySelector("#prev-page").disabled = state.page <= 0;
    document.querySelector("#next-page").disabled = state.page + 1 >= state.totalPages;
  } catch (error) {
    status.textContent = error.message;
    rows.replaceChildren();
  }
}

function renderRows() {
  const visible = state.products.filter((product) => {
    if (!state.query) return true;
    return `${product.sku} ${product.name}`.toLowerCase().includes(state.query);
  });

  if (!visible.length) {
    rows.innerHTML = `<tr><td class="empty" colspan="6">No matching products.</td></tr>`;
    return;
  }

  rows.replaceChildren(...visible.map((product) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${escapeHtml(product.sku)}</td>
      <td>${escapeHtml(product.name)}</td>
      <td>${formatMoney(product.price)}</td>
      <td>${product.stock}</td>
      <td><span class="badge ${product.active ? "" : "off"}">${product.active ? "Active" : "Inactive"}</span></td>
      <td class="actions">
        <button class="btn ghost" type="button" data-edit="${escapeHtml(product.id)}">Edit</button>
        <button class="btn ghost" type="button" data-delete="${escapeHtml(product.id)}">Delete</button>
      </td>`;
    row.querySelector("[data-edit]").addEventListener("click", () => fillForm(product));
    row.querySelector("[data-delete]").addEventListener("click", () => askDelete(product));
    return row;
  }));
}

async function saveProduct(event) {
  event.preventDefault();
  formError.textContent = "";
  const id = document.querySelector("#product-id").value;
  const payload = {
    sku: document.querySelector("#sku").value.trim(),
    name: document.querySelector("#name").value.trim(),
    description: document.querySelector("#description").value.trim(),
    price: Number(document.querySelector("#price").value),
    stock: Number(document.querySelector("#stock").value)
  };

  try {
    const response = await fetch(id ? `${api}/${id}` : api, {
      method: id ? "PUT" : "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });
    if (!response.ok) throw new Error(await readError(response));
    resetForm();
    if (!id) state.page = 0;
    await loadProducts();
  } catch (error) {
    formError.textContent = error.message;
  }
}

function fillForm(product) {
  document.querySelector("#product-id").value = product.id;
  document.querySelector("#sku").value = product.sku;
  document.querySelector("#name").value = product.name;
  document.querySelector("#description").value = product.description ?? "";
  document.querySelector("#price").value = product.price;
  document.querySelector("#stock").value = product.stock;
  formTitle.textContent = "Edit product";
  cancelEdit.hidden = false;
  document.querySelector("#editor").scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function resetForm() {
  form.reset();
  document.querySelector("#product-id").value = "";
  formError.textContent = "";
  formTitle.textContent = "New product";
  cancelEdit.hidden = true;
}

function askDelete(product) {
  state.pendingDeleteId = product.id;
  document.querySelector("#delete-copy").textContent = `Delete ${product.name} (${product.sku})?`;
  dialog.showModal();
}

async function confirmDelete(event) {
  if (event.target.value !== "confirm" || !state.pendingDeleteId) return;
  try {
    const response = await fetch(`${api}/${state.pendingDeleteId}`, { method: "DELETE" });
    if (!response.ok && response.status !== 204) throw new Error(await readError(response));
    if (document.querySelector("#product-id").value === state.pendingDeleteId) resetForm();
    state.pendingDeleteId = null;
    await loadProducts();
  } catch (error) {
    status.textContent = error.message;
  }
}

function changePage(delta) {
  const next = state.page + delta;
  if (next < 0 || next >= state.totalPages) return;
  state.page = next;
  loadProducts();
}

async function readError(response) {
  try {
    const body = await response.json();
    if (body.fieldErrors) {
      return Object.values(body.fieldErrors).join(" ");
    }
    return body.message || `Request failed (${response.status})`;
  } catch {
    return `Request failed (${response.status})`;
  }
}

function formatMoney(value) {
  return new Intl.NumberFormat(undefined, { style: "currency", currency: "USD" }).format(Number(value));
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
