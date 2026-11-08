package com.bidco.api_rest.dto;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;

public class FiltroDTO {
    private int minPrice;
    private int maxPrice;
    private String[] categorias;
    private String dateOrder;

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

    @Override
    public String toString() {
        return "Filtro: minPrice = " + minPrice + ", maxPrice = " + maxPrice + ", categorias = " + Arrays.toString(categorias) + ", dateOrder = " + dateOrder;
    }
}