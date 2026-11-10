package com.bidco.api_rest.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

public class FiltroDTO {
    public static final int TAMANO_MAXIMO = 100;

    private int minPrice;
    private int maxPrice;
    private String[] categorias;
    private String dateOrder;
    private int page;
    private int size;

    public FiltroDTO() {}

    public int getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(int minPrice) {
        this.minPrice = minPrice;
    }

    public int getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(int maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String[] getCategorias() {
        return categorias;
    }

    public void setCategorias(String[] categorias) {
        this.categorias = categorias;
    }

    public String getDateOrder() {
        return dateOrder == null ? "" : dateOrder;
    }

    /** Sin categorías marcadas no se filtra por categoría. */
    public boolean sinCategorias() {
        return categorias == null || categorias.length == 0;
    }

    /**
     * Categorías en la forma en que se guardan ("tecnologia", "moda"...): minúsculas y sin tildes.
     * Nunca devuelve un array vacío para que la cláusula IN de la consulta sea siempre válida.
     */
    public String[] categoriasNormalizadas() {
        if (sinCategorias()) {
            return new String[]{""};
        }
        return Arrays.stream(categorias)
                .filter(c -> c != null)
                .map(FiltroDTO::normalizar)
                .toArray(String[]::new);
    }

    static String normalizar(String categoria) {
        return Normalizer.normalize(categoria.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    public void setDateOrder(String dateOrder) {
        this.dateOrder = dateOrder;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    /**
     * Paginación opcional: sin {@code size} (o con un valor no positivo) no se pagina, como antes.
     * La página empieza en 0 y el tamaño se limita a {@link #TAMANO_MAXIMO}.
     */
    public Pageable paginacion() {
        if (size <= 0) {
            return Pageable.unpaged();
        }
        return PageRequest.of(Math.max(page, 0), Math.min(size, TAMANO_MAXIMO));
    }

    @Override
    public String toString() {
        return "Filtro: minPrice = " + minPrice + ", maxPrice = " + maxPrice + ", categorias = " + Arrays.toString(categorias) + ", dateOrder = " + dateOrder;
    }
}