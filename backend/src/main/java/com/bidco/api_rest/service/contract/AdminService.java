package com.bidco.api_rest.service.contract;

import com.bidco.api_rest.dto.admin.AdminLoginDTO;
import com.bidco.api_rest.dto.admin.SorteoCreateDTO;

import jakarta.servlet.http.HttpSession;

public interface AdminService {
    void login(AdminLoginDTO dto, HttpSession session);
    void createSorteo(SorteoCreateDTO dto, HttpSession session);

    /** Retira una subasta por moderación. Solo las cuentas de administración, no las de experto. */
    void retirarSubasta(Long subastaId, String motivo, HttpSession session);
}