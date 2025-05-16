package com.example.budget.service;

import com.example.budget.entity.Category;
import com.example.budget.entity.CategoryType;
import com.example.budget.exception.CategoryNotFoundException;
import com.example.budget.exception.InvalidCategoryException;
import com.example.budget.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Create a new category
     *
     * @param category the category to create
     * @return the created category
     * @throws InvalidCategoryException if the category is invalid or name already exists
     */
    @Transactional
    public Category createCategory(Category category) {
        validateCategory(category);
        validateUniqueName(category);
        return categoryRepository.save(category);
    }

    /**
     * Update an existing category
     *
     * @param id the id of the category to update
     * @param category the updated category data
     * @return the updated category
     * @throws CategoryNotFoundException if the category is not found
     * @throws InvalidCategoryException if the category is invalid or name already exists
     */
    @Transactional
    public Category updateCategory(Long id, Category category) {
        Category existingCategory = findById(id);
        
        // Update fields
        existingCategory.setName(category.getName());
        existingCategory.setDescription(category.getDescription());
        existingCategory.setType(category.getType());
        
        validateCategory(existingCategory);
        validateUniqueNameForUpdate(existingCategory);
        return categoryRepository.save(existingCategory);
    }

    /**
     * Delete a category by id
     *
     * @param id the id of the category to delete
     * @throws CategoryNotFoundException if the category is not found
     */
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findById(id);
        categoryRepository.delete(category);
    }

    /**
     * Find a category by id
     *
     * @param id the id of the category to find
     * @return the category
     * @throws CategoryNotFoundException if the category is not found
     */
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("Category not found with id: " + id));
    }

    /**
     * Find all categories
     *
     * @return list of all categories
     */
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }
    
    /**
     * Find categories by type
     *
     * @param type the type of categories to find
     * @return list of categories with the specified type
     */
    public List<Category> findByType(CategoryType type) {
        return categoryRepository.findByType(type);
    }
    
    /**
     * Validate category data
     *
     * @param category the category to validate
     * @throws InvalidCategoryException if the category is invalid
     */
    private void validateCategory(Category category) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            throw new InvalidCategoryException("Category name cannot be empty");
        }
        
        if (category.getType() == null) {
            throw new InvalidCategoryException("Category type cannot be null");
        }
    }
    
    /**
     * Validate that the category name is unique (for new categories)
     *
     * @param category the category to validate
     * @throws InvalidCategoryException if the category name already exists
     */
    private void validateUniqueName(Category category) {
        Optional<Category> existingCategory = categoryRepository.findByNameIgnoreCase(category.getName());
        if (existingCategory.isPresent()) {
            throw new InvalidCategoryException("Category with name '" + category.getName() + "' already exists");
        }
    }
    
    /**
     * Validate that the category name is unique (for updates)
     * Allows the category to keep its current name
     *
     * @param category the category to validate
     * @throws InvalidCategoryException if the category name already exists for a different category
     */
    private void validateUniqueNameForUpdate(Category category) {
        Optional<Category> existingCategory = categoryRepository.findByNameIgnoreCase(category.getName());
        if (existingCategory.isPresent() && !existingCategory.get().getId().equals(category.getId())) {
            throw new InvalidCategoryException("Category with name '" + category.getName() + "' already exists");
        }
    }
}