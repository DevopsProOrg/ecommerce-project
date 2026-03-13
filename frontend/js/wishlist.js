let wishlist = JSON.parse(localStorage.getItem("wishlist")) || [];

function toggleWishlistItem(id){

const index = wishlist.findIndex(p => p.id === id);

if(index > -1){
wishlist.splice(index,1);
}else{
const product = productsDB.find(p => p.id === id);
wishlist.push(product);
}

localStorage.setItem("wishlist", JSON.stringify(wishlist));

updateWishlistUI();
}