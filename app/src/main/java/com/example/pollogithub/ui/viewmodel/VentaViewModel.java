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

/**
 * Modelo de Vista para el Terminal de Venta (POS): VentaViewModel
 * 
 * Capa de Presentación / Arquitectura MVVM (Model-View-ViewModel)
 * Hereda de: AndroidViewModel
 * 
 * Centraliza la máquina de estados del carrito de compras y la interacción con el catálogo.
 * Sincroniza en tiempo real las cantidades seleccionadas en memoria con las entidades persistidas,
 * recalculando reactivamente los totales contables y gestionando la creación de pedidos.
 * 
 * Conceptos de Ingeniería de Software aplicados:
 * - Gestión de Estado en Memoria (State Management): Empleo de un mapa hash 'cartQuantities'
 *   como estructura de acceso O(1) para mantener el recuento de artículos agregados.
 * - Programación Reactiva con LiveData: Exposición de flujos diferenciados para el conteo de ítems,
 *   precio acumulado y visibilidad condicional de la barra de resumen de pedido.
 * - Desacoplamiento de Negocio: La lógica de acumulación, totales y armado del pedido reside en el ViewModel,
 *   manteniendo los Fragmentos y Actividades como simples renderizadores pasivos (Dumb Views).
 * 
 * @author Estudiante de Ingeniería de Sistemas (Proyecto Final / Taller de Grado)
 * @version 1.0
 */
public class VentaViewModel extends AndroidViewModel {

    /**
     * Instancia del repositorio de datos para operaciones transaccionales.
     */
    private final PosRepository repository;

    /**
     * Flujo mediador que fusiona el catálogo de base de datos con las cantidades en carrito.
     */
    private final MediatorLiveData<List<Product>> productsLiveData = new MediatorLiveData<>();

    /**
     * Diccionario en memoria (ID_Producto -> Cantidad) para control de artículos seleccionados.
     */
    private final Map<Integer, Integer> cartQuantities = new HashMap<>();

    /**
     * Contador observable del número total de unidades agregadas al pedido.
     */
    private final MutableLiveData<Integer> cartCount = new MutableLiveData<>(0);

    /**
     * Importe monetario observable acumulado del carrito.
     */
    private final MutableLiveData<Double> cartTotal = new MutableLiveData<>(0.0);

    /**
     * Bandera observable para mostrar u ocultar la barra flotante de checkout.
     */
    private final MutableLiveData<Boolean> isCartVisible = new MutableLiveData<>(false);

    /**
     * Caché en memoria de los productos actualmente desplegados en la vista.
     */
    private List<Product> currentProducts = new ArrayList<>();

    /**
     * Constructor del ViewModel. Conecta el observador reactivo al repositorio de productos.
     * 
     * @param application Contexto de aplicación Android.
     */
    public VentaViewModel(@NonNull Application application) {
        super(application);
        repository = PosRepository.getInstance(application);

        // Suscripción reactiva a la tabla de productos de Room
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
                            qty,
                            e.getImagenLocalPath()
                    ));
                }
                currentProducts = list;
                productsLiveData.setValue(list);
                recalculateCart();
            }
        });
    }

    /**
     * Mapeador estático de claves foráneas de categoría a denominaciones textuales en la vista.
     * 
     * @param catId Identificador numérico de categoría.
     * @return Etiqueta amigable para filtros visuales.
     */
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

    // ==========================================
    // GETTERS DE FLUJOS REACTIVOS (OBSERVABLES)
    // ==========================================

    public LiveData<List<Product>> getProductsLiveData() { return productsLiveData; }
    public LiveData<List<CategoriaEntity>> getCategoriasLiveData() { return repository.getCategoriasLiveData(); }
    public LiveData<Integer> getCartCount() { return cartCount; }
    public LiveData<Double> getCartTotal() { return cartTotal; }
    public LiveData<Boolean> getIsCartVisible() { return isCartVisible; }

    /**
     * Incrementa la cantidad de un producto dentro del carrito de compras.
     * Actualiza el diccionario en memoria y dispara la recalculación aritmética.
     * 
     * @param product Artículo seleccionado por el cajero.
     */
    public void addProductToCart(Product product) {
        int currentQty = cartQuantities.containsKey(product.getId()) ? cartQuantities.get(product.getId()) : 0;
        int newQty = currentQty + 1;
        cartQuantities.put(product.getId(), newQty);
        product.setQuantityInCart(newQty);

        recalculateCart();
    }

    /**
     * Decrementa la cantidad de un producto dentro del carrito de compras.
     * Si llega a cero, se remueve del carrito.
     * 
     * @param product Artículo a decrementar por el cajero.
     */
    public void removeProductFromCart(Product product) {
        int currentQty = cartQuantities.containsKey(product.getId()) ? cartQuantities.get(product.getId()) : 0;
        if (currentQty <= 0) return;
        int newQty = currentQty - 1;
        if (newQty > 0) {
            cartQuantities.put(product.getId(), newQty);
        } else {
            cartQuantities.remove(product.getId());
        }
        product.setQuantityInCart(newQty);

        recalculateCart();
    }

    /**
     * Restablece el carrito de compras, vaciando el estado en memoria y reseteando contadores a cero.
     */
    public void clearCart() {
        cartQuantities.clear();
        for (Product p : currentProducts) {
            p.setQuantityInCart(0);
        }
        recalculateCart();
        productsLiveData.setValue(currentProducts);
    }

    /**
     * Algoritmo de agregación contable del carrito.
     * Computa la sumatoria de unidades y el importe acumulado en moneda local.
     */
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

    /**
     * Extrae la lista de productos que presentan una cantidad activa mayor a cero en el carrito.
     * 
     * @return Colección de artículos con cantidad > 0.
     */
    public List<Product> getCartProducts() {
        List<Product> cart = new ArrayList<>();
        for (Product p : currentProducts) {
            if (p.getQuantityInCart() > 0) {
                cart.add(p);
            }
        }
        return cart;
    }

    /**
     * Orquesta la confirmación transaccional del pedido enviando los artículos del carrito al repositorio.
     * 
     * @param tipoEntrega Modalidad ("mesa", "para_llevar").
     * @param mesaId      Número de mesa (opcional).
     * @param callback    Callback para propagar el resultado o error hacia la interfaz gráfica.
     */
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
