package com.u.api.controller.user;

import com.u.api.dto.user.UserAddressDTO;
import com.u.api.service.user.UserAddressService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/addresses")
public class UserAddressController {

    private final UserAddressService addressService;

    public UserAddressController(UserAddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/{id}")
    public UserAddressDTO getAddress(@PathVariable Long id, @RequestParam Long userId) {
        return addressService.getAddress(id, userId);
    }
}