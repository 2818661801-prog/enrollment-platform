package com.enroll.server.service;

import com.enroll.server.dto.CategoryDTO;
import com.enroll.server.entity.Category;
import com.enroll.server.exception.BusinessException;
import com.enroll.server.dto.ResultCode;
import com.enroll.server.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** 列表（全部） */
    public List<CategoryDTO> list() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** 新增 */
    @Transactional
    public CategoryDTO create(String name) {
        name = name == null ? null : name.trim();
        if (name == null || name.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "类别名称不能为空");
        }
        if (categoryRepository.findByName(name).isPresent()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "类别已存在：" + name);
        }
        Category entity = new Category();
        entity.setName(name);
        categoryRepository.save(entity);
        return toDTO(entity);
    }

    /** 修改 */
    @Transactional
    public CategoryDTO update(Integer id, String name) {
        name = name == null ? null : name.trim();
        if (name == null || name.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "类别名称不能为空");
        }
        Category entity = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.CATEGORY_NOT_FOUND, "类别不存在"));
        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "类别已存在：" + name);
        }
        entity.setName(name);
        categoryRepository.save(entity);
        return toDTO(entity);
    }

    /** 删除 */
    @Transactional
    public void delete(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new BusinessException(ResultCode.CATEGORY_NOT_FOUND, "类别不存在");
        }
        categoryRepository.deleteById(id);
    }

    private CategoryDTO toDTO(Category entity) {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        return dto;
    }
}
