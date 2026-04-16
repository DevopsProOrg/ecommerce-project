let cart = JSON.parse(localStorage.getItem("cart")) || [];

function addToCart(id, quantity = 1) {

    if (!productsDB || productsDB.length === 0) {
        alert("Products not loaded yet, please wait...");
        return;
    }

    const product = productsDB.find(p => p.id === id);

    if (!product) {
        alert("Product not found!");
        return;
    }

    const existing = cart.find(p => p.id === id);

    if(existing){
        existing.quantity += quantity;
    } else {
        cart.push({...product, quantity});
    }

    localStorage.setItem("cart", JSON.stringify(cart));
    updateCartUI();
}

function removeFromCart(id){

cart = cart.filter(p => p.id !== id);

localStorage.setItem("cart", JSON.stringify(cart));

updateCartUI();
}