package com.enroll.server.repository;

import com.enroll.server.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    /** 按名称查找（用于新增时校验重名） */
    Optional<Category> findByName(String name);

    /** 名称唯一约束：是否存在同名记录（排除指定 id） */
    boolean existsByNameAndIdNot(String name, Integer id);
}
