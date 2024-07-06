# Bugs
* AISearchActivity Low Accuracy
* Crashing after editing product
* Crashing after deleting product
* Can't edit location
* Search place doesn't work. Need Credit Card for API key

# Features
* Show nearby products of chosen location.
* Admin permission required before adding a product.
* When product is added, an email will be sent to the user notifying them.
* You can see the owner of the product in product details
* Users can chat with owner

# System Design
* In MainActivity we decide who's User and who's Admin.
* Admin can grant permission for adding product. Admin can also remove products and users
* Show products assuming that you're a user
* Admins can't login as user and vice versa