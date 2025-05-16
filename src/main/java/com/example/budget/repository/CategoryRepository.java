package com.example.budget.repository;

import com.example.budget.entity.Category;
import com.example.budget.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByType(CategoryType type);
    List<Category> findByNameContainingIgnoreCase(String name);
    Optional<Category> findByNameIgnoreCase(String name);
}