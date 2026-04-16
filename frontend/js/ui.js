function renderProducts() {

  const grid = document.getElementById("product-grid");
  grid.innerHTML = "";

  const filtered = productsDB.filter(product => {

    return (
      (currentFilters.category === "All" || product.category === currentFilters.category) &&
      product.price <= currentFilters.priceMax
    );
  });

  filtered.forEach(product => {
    grid.innerHTML += `
      <div class="product-card">
        <img src="${product.image}">
        <h3>${product.name}</h3>
        <p>₹${product.price}</p>

        <button onclick="addToCart(${product.id})">Add to Cart</button>
        <button onclick="toggleWishlistItem(${product.id})">Wishlist</button>
      </div>
    `;
  });
}