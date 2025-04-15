// Function to update the cart dynamically
function updateCart(medicineId, quantity) {
    const formData = new FormData();
    formData.append('medicineId', medicineId);
    formData.append('quantity', quantity);

    fetch('/cart/update', {
        method: 'POST',
        body: formData
    })
    .then(response => response.text())
    .then(data => {
        document.getElementById("cart-items").innerHTML = data;
        alert("Cart updated successfully!");
    })
    .catch(error => {
        console.error("Error updating cart:", error);
        alert("Failed to update cart.");
    });
}

// Function to handle checkout confirmation
function confirmCheckout() {
    const confirmAction = confirm("Are you sure you want to proceed to checkout?");
    if (confirmAction) {
        window.location.href = '/cart/checkout';
    }
}

// Function to remove an item from the cart
function removeItemFromCart(medicineId) {
    const formData = new FormData();
    formData.append('medicineId', medicineId);

    fetch('/cart/remove', {
        method: 'POST',
        body: formData
    })
    .then(response => response.text())
    .then(data => {
        document.getElementById("cart-items").innerHTML = data;
        alert("Item removed from cart.");
    })
    .catch(error => {
        console.error("Error removing item:", error);
        alert("Failed to remove item from cart.");
    });
}

// Helper function to format the price
function formatPrice(price) {
    return "$" + parseFloat(price).toFixed(2);
}

// Event listeners for dynamic cart updates
document.addEventListener('DOMContentLoaded', function() {
    // Example: If you have "Add to Cart" buttons in your medicine grid
    const addButtons = document.querySelectorAll('.add-to-cart-btn');
    addButtons.forEach(button => {
        button.addEventListener('click', function(event) {
            const medicineId = event.target.dataset.medicineId;
            const quantity = 1;  // You can adjust this based on your UI
            updateCart(medicineId, quantity);
        });
    });

    // Event listener for Checkout button
    const checkoutButton = document.getElementById('checkout-btn');
    if (checkoutButton) {
        checkoutButton.addEventListener('click', function(event) {
            confirmCheckout();
        });
    }
});
