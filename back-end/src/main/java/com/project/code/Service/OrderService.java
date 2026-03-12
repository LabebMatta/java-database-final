package com.project.code.Service;

import com.project.code.Model.Customer;
import com.project.code.Model.Inventory;
import com.project.code.Model.OrderDetails;
import com.project.code.Model.OrderItem;
import com.project.code.Model.PlaceOrderRequestDTO;
import com.project.code.Model.Product;
import com.project.code.Model.PurchaseProductDTO;
import com.project.code.Model.Store;
import com.project.code.Repo.CustomerRepository;
import com.project.code.Repo.InventoryRepository;
import com.project.code.Repo.OrderDetailsRepository;
import com.project.code.Repo.OrderItemRepository;
import com.project.code.Repo.ProductRepository;
import com.project.code.Repo.StoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private OrderDetailsRepository orderDetailsRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    public void saveOrder(PlaceOrderRequestDTO placeOrderRequest) {

        Customer customer = customerRepository.findByEmail(placeOrderRequest.getCustomerEmail());
        if (customer == null) {
            customer = new Customer();
            customer.setName(placeOrderRequest.getCustomerName());
            customer.setEmail(placeOrderRequest.getCustomerEmail());
            customer.setPhone(placeOrderRequest.getCustomerPhone());
            customer = customerRepository.save(customer);
        }

        Optional<Store> storeOptional = storeRepository.findById(placeOrderRequest.getStoreId());
        if (!storeOptional.isPresent()) {
            throw new RuntimeException("Store not found with id: " + placeOrderRequest.getStoreId());
        }
        Store store = storeOptional.get();

        OrderDetails orderDetails = new OrderDetails(
                customer,
                store,
                placeOrderRequest.getTotalPrice(),
                LocalDateTime.now()
        );
        orderDetails = orderDetailsRepository.save(orderDetails);

        for (PurchaseProductDTO purchaseProduct: placeOrderRequest.getPurchaseProducts()) {
            Product product = productRepository.findById((long) purchaseProduct.getId());

            Inventory inventory = inventoryRepository.findByProductIdandStoreId(
                    purchaseProduct.getId(),
                    placeOrderRequest.getStoreId()
            );
            if (inventory != null) {
                inventory.setStockLevel(inventory.getStockLevel() - purchaseProduct.getQuantity());
                inventoryRepository.save(inventory);
            }

            OrderItem orderItem = new OrderItem(
                    orderDetails,
                    product,
                    purchaseProduct.getQuantity(),
                    purchaseProduct.getPrice()
            );
            orderItemRepository.save(orderItem);
        }
    }
}
