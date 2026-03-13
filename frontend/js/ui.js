let currentFilters = {
  category: "All",
  priceMax: 5000,
  search: ""
};
function applyFilters() {

  const checked = document.querySelector('input[name="category"]:checked');
  currentFilters.category = checked ? checked.value : "All";

  currentFilters.priceMax =
    parseInt(document.getElementById("price-range").value);

  renderProducts();
}

function renderProducts() {

  const grid = document.getElementById("product-grid");
  grid.innerHTML = "";

  const filtered = productsDB.filter(product => {

    const categoryMatch =
      currentFilters.category === "All" ||
      product.category === currentFilters.category;

    const priceMatch =
      product.price <= currentFilters.priceMax;

    return categoryMatch && priceMatch;

  });

  filtered.forEach(product => {

    grid.innerHTML += `
      <div class="product-card">

        <img src="${product.image}">

        <h3>${product.name}</h3>

        <p>₹${product.price}</p>

        <button onclick="addToCart(${product.id})">
        Add to Cart
        </button>

        <button onclick="toggleWishlistItem(${product.id})">
        Wishlist
        </button>

      </div>
    `;

  });

}
function updatePriceLabel(){

  const value = document.getElementById("price-range").value;

  document.getElementById("price-label").innerText =
    "₹" + value;

}
