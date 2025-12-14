package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

import java.util.List;

public interface DishService {
    void save(DishDTO dto);

    PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    void deleteBathch(List<Long> ids);

    DishVO getByIdWithFlavors(Long id);

    void updatewithFlavors(DishDTO dishDTO);
}
