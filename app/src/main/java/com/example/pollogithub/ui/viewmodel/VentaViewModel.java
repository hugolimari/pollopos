package com.example.pollogithub.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pollogithub.Product;
import com.example.pollogithub.data.entity.CategoriaEntity;
import com.example.pollogithub.data.entity.PedidoEntity;
import com.example.pollogithub.data.entity.ProductoEntity;
import com.example.pollogithub.data.repository.PosRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VentaViewModel extends AndroidViewModel {

    private final PosRepository repository;
    private final MediatorLiveData<List<Product>> productsLiveData = new MediatorLiveData<>();
    private final Map<Integer, Integer> cartQuantities = new HashMap<>();

    private final MutableLiveData<Integer> cartCount = new MutableLiveData<>(0);
    private final MutableLiveData<Double> cartTotal = new MutableLiveData<>(0.0);
    private final MutableLiveData<Boolean> isCartVisible = new MutableLiveData<>(false);

    private List<Product> currentProducts = new ArrayList<>();

    public VentaViewModel(@NonNull Application application) {
        super(application);
        repository = PosRepository.getInstance(application);

        LiveData<List<ProductoEntity>> dbProducts = repository.getProductosLiveData();
        productsLiveData.addSource(dbProducts, entities -> {
            if (entities != null) {
                List<Product> list = new ArrayList<>();
                for (ProductoEntity e : entities) {
                    int qty = cartQuantities.containsKey(e.getId()) ? cartQuantities.get(e.getId()) : 0;
                    String catName = getCategoryName(e.getCategoriaId());
                    list.add(new Product(
                            e.getId(),
                            e.getNombre(),
                            e.getDescripcion(),
                            e.getPrecio(),
                            e.getEmoji(),
                            catName,
                            e.getThumbDrawableRes(),
                            !e.isDisponible(),
                            qty
                    ));
                }
                currentProducts = list;
                productsLiveData.setValue(list);
                recalculateCart();
            }
        });
    }

    private String getCategoryName(int catId) {
        switch (catId) {
            case 1: return "Pollo frito";
            case 2: return "A la brasa";
            case 3: return "Combos";
            case 4: return "Bebidas";
            case 5: return "Acompañamientos";
            default: return "Todos";
        }
    }

    public LiveData<List<Product>> getProductsLiveData() { return productsLiveData; }
    public LiveData<List<CategoriaEntity>> getCategoriasLiveData() { return repository.getCategoriasLiveData(); }
    public LiveData<Integer> getCartCount() { return cartCount; }
    public LiveData<Double> getCartTotal() { return cartTotal; }
    public LiveData<Boolean> getIsCartVisible() { return isCartVisible; }

    public void addProductToCart(Product product) {
        int currentQty = cartQuantities.containsKey(product.getId()) ? cartQuantities.get(product.getId()) : 0;
        int newQty = currentQty + 1;
        cartQuantities.put(product.getId(), newQty);
        product.setQuantityInCart(newQty);

        recalculateCart();
    }

    public void clearCart() {
        cartQuantities.clear();
        for (Product p : currentProducts) {
            p.setQuantityInCart(0);
        }
        recalculateCart();
        productsLiveData.setValue(currentProducts);
    }

    private void recalculateCart() {
        int count = 0;
        double total = 0.0;

        for (Product p : currentProducts) {
            int qty = cartQuantities.containsKey(p.getId()) ? cartQuantities.get(p.getId()) : 0;
            if (qty > 0) {
                count += qty;
                total += qty * p.getPrice();
            }
        }

        cartCount.setValue(count);
        cartTotal.setValue(total);
        isCartVisible.setValue(count > 0);
    }

    public List<Product> getCartProducts() {
        List<Product> cart = new ArrayList<>();
        for (Product p : currentProducts) {
            if (p.getQuantityInCart() > 0) {
                cart.add(p);
            }
        }
        return cart;
    }

    public void confirmarPedido(String tipoEntrega, Integer mesaId, PosRepository.Callback<PedidoEntity> callback) {
        List<Product> cart = getCartProducts();
        if (cart.isEmpty()) {
            callback.onError("El carrito está vacío");
            return;
        }

        repository.crearPedido(tipoEntrega, mesaId, cart, new PosRepository.Callback<PedidoEntity>() {
            @Override
            public void onSuccess(PedidoEntity result) {
                clearCart();
                callback.onSuccess(result);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
}
