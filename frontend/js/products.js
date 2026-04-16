let productsDB = [];

async function loadProducts() {
    const res = await fetch("http://localhost:8888/api/products");

    productsDB = await res.json();

    console.log("Products:", productsDB);

    renderProducts(); // call UI after data
}