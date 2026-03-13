let cart = JSON.parse(localStorage.getItem("cart")) || [];

function addToCart(id, quantity = 1) {

const product = productsDB.find(p => p.id === id);

const existing = cart.find(p => p.id === id);

if(existing){
existing.quantity += quantity;
}else{
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