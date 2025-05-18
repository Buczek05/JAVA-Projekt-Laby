package com.example.budget.service;

import com.example.budget.entity.Category;
import com.example.budget.entity.CategoryType;
import com.example.budget.exception.CategoryNotFoundException;
import com.example.budget.exception.DuplicateCategoryException;
import com.example.budget.exception.InvalidCategoryException;
import com.example.budget.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category validCategory;
    private Category savedCategory;

    @BeforeEach
    void setUp() {
        validCategory = new Category();
        validCategory.setName("Test Category");
        validCategory.setDescription("Test Description");
        validCategory.setType(CategoryType.EXPENSE);

        savedCategory = new Category();
        savedCategory.setId(1L);
        savedCategory.setName("Test Category");
        savedCategory.setDescription("Test Description");
        savedCategory.setType(CategoryType.EXPENSE);
    }

    @Test
    void createCategory_ValidCategory_ReturnsCreatedCategory() {
        when(categoryRepository.findByNameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        Category result = categoryService.createCategory(validCategory);

        assertNotNull(result);
        assertEquals(savedCategory.getId(), result.getId());
        assertEquals(savedCategory.getName(), result.getName());
        assertEquals(savedCategory.getDescription(), result.getDescription());
        assertEquals(savedCategory.getType(), result.getType());
        verify(categoryRepository, times(1)).findByNameIgnoreCase(validCategory.getName());
        verify(categoryRepository, times(1)).save(validCategory);
    }

    @Test
    void createCategory_NullName_ThrowsInvalidCategoryException() {
        validCategory.setName(null);

        InvalidCategoryException exception = assertThrows(InvalidCategoryException.class, 
            () -> categoryService.createCategory(validCategory));
        assertEquals("Category name cannot be empty", exception.getMessage());
        verify(categoryRepository, never()).findByNameIgnoreCase(anyString());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createCategory_EmptyName_ThrowsInvalidCategoryException() {
        validCategory.setName("");

        InvalidCategoryException exception = assertThrows(InvalidCategoryException.class, 
            () -> categoryService.createCategory(validCategory));
        assertEquals("Category name cannot be empty", exception.getMessage());
        verify(categoryRepository, never()).findByNameIgnoreCase(anyString());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createCategory_NullType_ThrowsInvalidCategoryException() {
        validCategory.setType(null);

        InvalidCategoryException exception = assertThrows(InvalidCategoryException.class, 
            () -> categoryService.createCategory(validCategory));
        assertEquals("Category type cannot be null", exception.getMessage());
        verify(categoryRepository, never()).findByNameIgnoreCase(anyString());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createCategory_DuplicateName_ThrowsDuplicateCategoryException() {
        when(categoryRepository.findByNameIgnoreCase(validCategory.getName())).thenReturn(Optional.of(savedCategory));

        DuplicateCategoryException exception = assertThrows(DuplicateCategoryException.class, 
            () -> categoryService.createCategory(validCategory));
        assertEquals("Category with name '" + validCategory.getName() + "' already exists", exception.getMessage());
        verify(categoryRepository, times(1)).findByNameIgnoreCase(validCategory.getName());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_ValidCategory_ReturnsUpdatedCategory() {
        Long categoryId = 1L;
        Category updatedCategory = new Category();
        updatedCategory.setName("Updated Category");
        updatedCategory.setDescription("Updated Description");
        updatedCategory.setType(CategoryType.INCOME);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(savedCategory));
        when(categoryRepository.findByNameIgnoreCase(updatedCategory.getName())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        Category result = categoryService.updateCategory(categoryId, updatedCategory);

        assertNotNull(result);
        assertEquals(savedCategory.getId(), result.getId());
        assertEquals(updatedCategory.getName(), savedCategory.getName());
        assertEquals(updatedCategory.getDescription(), savedCategory.getDescription());
        assertEquals(updatedCategory.getType(), savedCategory.getType());
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, times(1)).findByNameIgnoreCase(updatedCategory.getName());
        verify(categoryRepository, times(1)).save(savedCategory);
    }

    @Test
    void updateCategory_NonExistentCategory_ThrowsCategoryNotFoundException() {
        Long categoryId = 999L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, 
            () -> categoryService.updateCategory(categoryId, validCategory));
        assertEquals("Category not found with id: " + categoryId, exception.getMessage());
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, never()).findByNameIgnoreCase(anyString());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_DuplicateName_ThrowsDuplicateCategoryException() {
        Long categoryId = 1L;
        Category existingCategory = new Category();
        existingCategory.setId(2L);
        existingCategory.setName("Updated Category");
        existingCategory.setType(CategoryType.EXPENSE);

        Category updatedCategory = new Category();
        updatedCategory.setName("Updated Category");
        updatedCategory.setDescription("Updated Description");
        updatedCategory.setType(CategoryType.INCOME);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(savedCategory));
        when(categoryRepository.findByNameIgnoreCase(updatedCategory.getName())).thenReturn(Optional.of(existingCategory));

        DuplicateCategoryException exception = assertThrows(DuplicateCategoryException.class, 
            () -> categoryService.updateCategory(categoryId, updatedCategory));
        assertEquals("Category with name '" + updatedCategory.getName() + "' already exists", exception.getMessage());
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, times(1)).findByNameIgnoreCase(updatedCategory.getName());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_SameNameDifferentId_AllowsUpdate() {
        Long categoryId = 1L;
        Category existingCategory = new Category();
        existingCategory.setId(1L);
        existingCategory.setName("Updated Category");
        existingCategory.setType(CategoryType.EXPENSE);

        Category updatedCategory = new Category();
        updatedCategory.setName("Updated Category");
        updatedCategory.setDescription("Updated Description");
        updatedCategory.setType(CategoryType.INCOME);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(savedCategory));
        when(categoryRepository.findByNameIgnoreCase(updatedCategory.getName())).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        Category result = categoryService.updateCategory(categoryId, updatedCategory);

        assertNotNull(result);
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, times(1)).findByNameIgnoreCase(updatedCategory.getName());
        verify(categoryRepository, times(1)).save(savedCategory);
    }

    @Test
    void deleteCategory_ExistingCategory_DeletesCategory() {
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(savedCategory));
        doNothing().when(categoryRepository).delete(savedCategory);

        categoryService.deleteCategory(categoryId);

        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, times(1)).delete(savedCategory);
    }

    @Test
    void deleteCategory_NonExistentCategory_ThrowsCategoryNotFoundException() {
        Long categoryId = 999L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, 
            () -> categoryService.deleteCategory(categoryId));
        assertEquals("Category not found with id: " + categoryId, exception.getMessage());
        verify(categoryRepository, times(1)).findById(categoryId);
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void findById_ExistingCategory_ReturnsCategory() {
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(savedCategory));

        Category result = categoryService.findById(categoryId);

        assertNotNull(result);
        assertEquals(savedCategory.getId(), result.getId());
        assertEquals(savedCategory.getName(), result.getName());
        assertEquals(savedCategory.getDescription(), result.getDescription());
        assertEquals(savedCategory.getType(), result.getType());
        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @Test
    void findById_NonExistentCategory_ThrowsCategoryNotFoundException() {
        Long categoryId = 999L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        CategoryNotFoundException exception = assertThrows(CategoryNotFoundException.class, 
            () -> categoryService.findById(categoryId));
        assertEquals("Category not found with id: " + categoryId, exception.getMessage());
        verify(categoryRepository, times(1)).findById(categoryId);
    }

    @Test
    void findAll_ReturnsAllCategories() {
        Category category1 = new Category();
        category1.setId(1L);
        category1.setName("Category 1");
        category1.setType(CategoryType.EXPENSE);

        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("Category 2");
        category2.setType(CategoryType.INCOME);

        List<Category> categories = Arrays.asList(category1, category2);
        when(categoryRepository.findAll()).thenReturn(categories);

        List<Category> result = categoryService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(category1.getId(), result.get(0).getId());
        assertEquals(category2.getId(), result.get(1).getId());
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    void findByType_ReturnsMatchingCategories() {
        Category category1 = new Category();
        category1.setId(1L);
        category1.setName("Category 1");
        category1.setType(CategoryType.EXPENSE);

        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("Category 2");
        category2.setType(CategoryType.EXPENSE);

        List<Category> categories = Arrays.asList(category1, category2);
        when(categoryRepository.findByType(CategoryType.EXPENSE)).thenReturn(categories);

        List<Category> result = categoryService.findByType(CategoryType.EXPENSE);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(category1.getId(), result.get(0).getId());
        assertEquals(category2.getId(), result.get(1).getId());
        assertEquals(CategoryType.EXPENSE, result.get(0).getType());
        assertEquals(CategoryType.EXPENSE, result.get(1).getType());
        verify(categoryRepository, times(1)).findByType(CategoryType.EXPENSE);
    }
}