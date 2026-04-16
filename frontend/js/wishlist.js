// Load wishlist from localStorage
let wishlist = JSON.parse(localStorage.getItem("wishlist")) || [];

// Add / Remove item from wishlist
function toggleWishlistItem(id) {

    // 🔴 Ensure products are loaded first
    if (!productsDB || productsDB.length === 0) {
        alert("Products not loaded yet, please wait...");
        return;
    }

    const index = wishlist.findIndex(p => p.id === id);

    if (index > -1) {
        // Remove from wishlist
        wishlist.splice(index, 1);
    } else {
        // Add to wishlist
        const product = productsDB.find(p => p.id === id);

        if (!product) {
            console.error("Product not found for wishlist");
            return;
        }

        wishlist.push(product);
    }

    // Save to localStorage
    localStorage.setItem("wishlist", JSON.stringify(wishlist));

    // Update UI
    updateWishlistUI();
}


// Optional: Update wishlist count or UI
function updateWishlistUI() {

    const countElement = document.getElementById("wishlist-count");

    if (countElement) {
        countElement.innerText = wishlist.length;
    }

    console.log("Wishlist:", wishlist);
}