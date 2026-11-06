package com.observability.monitor.service.inventory;

import com.observability.monitor.exception.BusinessRuleException;
import com.observability.monitor.exception.ResourceNotFoundException;
import com.observability.monitor.service.support.AbstractManagedService;
import com.observability.monitor.service.support.LatencySimulator;
import com.observability.monitor.service.support.OperationParameters;
import com.observability.monitor.service.support.ServiceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.UnaryOperator;

/**
 * Manages the product catalog and stock levels. All stock mutations are atomic per SKU.
 */
@Service
public class InventoryService extends AbstractManagedService {

    public static final String SERVICE_ID = "inventory";

    public static final String LIST_PRODUCTS = "list-products";
    public static final String GET_PRODUCT = "get-product";
    public static final String CHECK_STOCK = "check-stock";
    public static final String RESERVE_STOCK = "reserve-stock";
    public static final String RELEASE_STOCK = "release-stock";
    public static final String RESTOCK = "restock";

    private final ConcurrentMap<String, Product> products = new ConcurrentHashMap<>();

    @Autowired
    public InventoryService() {
        this(new LatencySimulator(15, 90));
    }

    public InventoryService(LatencySimulator latency) {
        super(latency);
        seedCatalog();

        register(LIST_PRODUCTS, this::listProducts);
        register(GET_PRODUCT, this::getProduct);
        register(CHECK_STOCK, this::checkStock);
        register(RESERVE_STOCK, this::reserveStock);
        register(RELEASE_STOCK, this::releaseStock);
        register(RESTOCK, this::restock);
    }

    @Override
    public String serviceId() {
        return SERVICE_ID;
    }

    @Override
    public String displayName() {
        return "Inventory Service";
    }

    public List<String> skus() {
        return products.keySet().stream().sorted().toList();
    }

    public Optional<Product> findProduct(String sku) {
        return Optional.ofNullable(products.get(sku));
    }

    private ServiceResponse listProducts(OperationParameters params) {
        List<Product> catalog = products.values().stream()
                .filter(product -> params.optionalText("category")
                        .map(category -> product.category().equalsIgnoreCase(category))
                        .orElse(true))
                .sorted(Comparator.comparing(Product::sku))
                .toList();
        return ServiceResponse.of("%d products found".formatted(catalog.size()), catalog);
    }

    private ServiceResponse getProduct(OperationParameters params) {
        Product product = requireProduct(params.requireText("sku"));
        return ServiceResponse.of("Product retrieved", product);
    }

    private ServiceResponse checkStock(OperationParameters params) {
        Product product = requireProduct(params.requireText("sku"));
        int requested = params.requirePositiveInt("quantity");

        Map<String, Object> availability = new LinkedHashMap<>();
        availability.put("sku", product.sku());
        availability.put("requestedQuantity", requested);
        availability.put("availableQuantity", product.availableQuantity());
        availability.put("available", product.availableQuantity() >= requested);

        String message = product.availableQuantity() >= requested ? "Stock available" : "Insufficient stock";
        return ServiceResponse.of(message, availability);
    }

    private ServiceResponse reserveStock(OperationParameters params) {
        String sku = params.requireText("sku");
        int quantity = params.requirePositiveInt("quantity");

        Product updated = updateStock(sku, current -> {
            if (current.availableQuantity() < quantity) {
                throw new BusinessRuleException("Insufficient stock for SKU '%s': requested %d, available %d"
                        .formatted(sku, quantity, current.availableQuantity()));
            }
            return current.withStock(current.availableQuantity() - quantity, current.reservedQuantity() + quantity);
        });
        return ServiceResponse.of("%d units reserved".formatted(quantity), updated);
    }

    private ServiceResponse releaseStock(OperationParameters params) {
        String sku = params.requireText("sku");
        int quantity = params.requirePositiveInt("quantity");

        Product updated = updateStock(sku, current -> {
            if (current.reservedQuantity() < quantity) {
                throw new BusinessRuleException("Cannot release %d units of SKU '%s': only %d reserved"
                        .formatted(quantity, sku, current.reservedQuantity()));
            }
            return current.withStock(current.availableQuantity() + quantity, current.reservedQuantity() - quantity);
        });
        return ServiceResponse.of("%d units released".formatted(quantity), updated);
    }

    private ServiceResponse restock(OperationParameters params) {
        String sku = params.requireText("sku");
        int quantity = params.requirePositiveInt("quantity");

        Product updated = updateStock(sku,
                current -> current.withStock(current.availableQuantity() + quantity, current.reservedQuantity()));
        return ServiceResponse.of("%d units added to stock".formatted(quantity), updated);
    }

    private Product updateStock(String sku, UnaryOperator<Product> mutation) {
        Product updated = products.computeIfPresent(sku, (key, current) -> mutation.apply(current));
        if (updated == null) {
            throw new ResourceNotFoundException("Product", sku);
        }
        return updated;
    }

    private Product requireProduct(String sku) {
        return findProduct(sku).orElseThrow(() -> new ResourceNotFoundException("Product", sku));
    }

    private void seedCatalog() {
        List.of(
                new Product("SKU-1001", "Mechanical Keyboard", "Peripherals", new BigDecimal("89.90"), 120, 0),
                new Product("SKU-1002", "Wireless Mouse", "Peripherals", new BigDecimal("34.50"), 300, 0),
                new Product("SKU-1003", "27\" 4K Monitor", "Displays", new BigDecimal("379.00"), 45, 0),
                new Product("SKU-1004", "USB-C Docking Station", "Accessories", new BigDecimal("149.99"), 80, 0),
                new Product("SKU-1005", "Noise Cancelling Headset", "Audio", new BigDecimal("199.00"), 60, 0),
                new Product("SKU-1006", "1080p Webcam", "Video", new BigDecimal("69.90"), 150, 0),
                new Product("SKU-1007", "NVMe SSD 1TB", "Storage", new BigDecimal("109.00"), 200, 0),
                new Product("SKU-1008", "Ergonomic Laptop Stand", "Accessories", new BigDecimal("45.00"), 25, 0)
        ).forEach(product -> products.put(product.sku(), product));
    }
}
