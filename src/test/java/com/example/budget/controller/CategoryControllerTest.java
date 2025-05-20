package com.example.budget.controller;

import com.example.budget.entity.Category;
import com.example.budget.entity.CategoryType;
import com.example.budget.exception.CategoryNotFoundException;
import com.example.budget.exception.DuplicateCategoryException;
import com.example.budget.exception.InvalidCategoryException;
import com.example.budget.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private Category testCategory;
    private Category updatedCategory;

    @ControllerAdvice
    static class TestControllerAdvice {

        @ExceptionHandler(CategoryNotFoundException.class)
        @ResponseStatus(HttpStatus.NOT_FOUND)
        public ResponseEntity<String> handleCategoryNotFoundException(CategoryNotFoundException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(InvalidCategoryException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<String> handleInvalidCategoryException(InvalidCategoryException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(DuplicateCategoryException.class)
        @ResponseStatus(HttpStatus.BAD_REQUEST)
        public ResponseEntity<String> handleDuplicateCategoryException(DuplicateCategoryException ex) {
            return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(categoryController)
                .setControllerAdvice(new TestControllerAdvice())
                .build();

        // Setup test category
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Test Category");
        testCategory.setDescription("Test Description");
        testCategory.setType(CategoryType.EXPENSE);

        // Setup updated category
        updatedCategory = new Category();
        updatedCategory.setId(1L);
        updatedCategory.setName("Updated Category");
        updatedCategory.setDescription("Updated Description");
        updatedCategory.setType(CategoryType.INCOME);
    }

    @Test
    void getAllCategories_ReturnsListOfCategories() throws Exception {
        // Given
        Category category1 = new Category();
        category1.setId(1L);
        category1.setName("Category 1");
        category1.setDescription("Description 1");
        category1.setType(CategoryType.EXPENSE);

        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("Category 2");
        category2.setDescription("Description 2");
        category2.setType(CategoryType.INCOME);

        List<Category> categories = Arrays.asList(category1, category2);

        when(categoryService.findAll()).thenReturn(categories);

        // When
        ResultActions result = mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Category 1")))
                .andExpect(jsonPath("$[0].description", is("Description 1")))
                .andExpect(jsonPath("$[0].type", is("EXPENSE")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].name", is("Category 2")))
                .andExpect(jsonPath("$[1].description", is("Description 2")))
                .andExpect(jsonPath("$[1].type", is("INCOME")));

        verify(categoryService, times(1)).findAll();
    }

    @Test
    void getCategoryById_ExistingCategory_ReturnsCategory() throws Exception {
        // Given
        when(categoryService.findById(1L)).thenReturn(testCategory);

        // When
        ResultActions result = mockMvc.perform(get("/api/categories/1")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Category")))
                .andExpect(jsonPath("$.description", is("Test Description")))
                .andExpect(jsonPath("$.type", is("EXPENSE")));

        verify(categoryService, times(1)).findById(1L);
    }

    @Test
    void getCategoryById_NonExistingCategory_ReturnsNotFound() throws Exception {
        // Given
        when(categoryService.findById(999L)).thenThrow(new CategoryNotFoundException("Category not found with id: 999"));

        // When
        ResultActions result = mockMvc.perform(get("/api/categories/999")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNotFound());

        verify(categoryService, times(1)).findById(999L);
    }

    @Test
    void getCategoriesByType_ReturnsMatchingCategories() throws Exception {
        // Given
        Category category1 = new Category();
        category1.setId(1L);
        category1.setName("Category 1");
        category1.setDescription("Description 1");
        category1.setType(CategoryType.EXPENSE);

        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("Category 2");
        category2.setDescription("Description 2");
        category2.setType(CategoryType.EXPENSE);

        List<Category> categories = Arrays.asList(category1, category2);

        when(categoryService.findByType(CategoryType.EXPENSE)).thenReturn(categories);

        // When
        ResultActions result = mockMvc.perform(get("/api/categories/type/EXPENSE")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].name", is("Category 1")))
                .andExpect(jsonPath("$[0].description", is("Description 1")))
                .andExpect(jsonPath("$[0].type", is("EXPENSE")))
                .andExpect(jsonPath("$[1].id", is(2)))
                .andExpect(jsonPath("$[1].name", is("Category 2")))
                .andExpect(jsonPath("$[1].description", is("Description 2")))
                .andExpect(jsonPath("$[1].type", is("EXPENSE")));

        verify(categoryService, times(1)).findByType(CategoryType.EXPENSE);
    }

    @Test
    void createCategory_ValidCategory_ReturnsCreatedCategory() throws Exception {
        // Given
        Category newCategory = new Category();
        newCategory.setName("New Category");
        newCategory.setDescription("New Description");
        newCategory.setType(CategoryType.INCOME);

        when(categoryService.createCategory(any(Category.class))).thenReturn(testCategory);

        // When
        ResultActions result = mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCategory)));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Category")))
                .andExpect(jsonPath("$.description", is("Test Description")))
                .andExpect(jsonPath("$.type", is("EXPENSE")));

        verify(categoryService, times(1)).createCategory(any(Category.class));
    }

    @Test
    void createCategory_InvalidCategory_ReturnsBadRequest() throws Exception {
        // Given
        Category invalidCategory = new Category();
        invalidCategory.setName("");
        invalidCategory.setDescription("Invalid Description");
        invalidCategory.setType(null);

        // When
        ResultActions result = mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCategory)));

        // Then
        result.andExpect(status().isBadRequest());

        // Verify that service was never called due to validation failure
        verify(categoryService, never()).createCategory(any(Category.class));
    }

    @Test
    void createCategory_DuplicateName_ReturnsBadRequest() throws Exception {
        // Given
        Category duplicateCategory = new Category();
        duplicateCategory.setName("Existing Category");
        duplicateCategory.setDescription("Description");
        duplicateCategory.setType(CategoryType.EXPENSE);

        when(categoryService.createCategory(any(Category.class)))
                .thenThrow(new DuplicateCategoryException("Category with name 'Existing Category' already exists"));

        // When
        ResultActions result = mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateCategory)));

        // Then
        result.andExpect(status().isBadRequest());

        verify(categoryService, times(1)).createCategory(any(Category.class));
    }

    @Test
    void updateCategory_ExistingCategory_ReturnsUpdatedCategory() throws Exception {
        // Given
        when(categoryService.updateCategory(eq(1L), any(Category.class))).thenReturn(updatedCategory);

        // When
        ResultActions result = mockMvc.perform(put("/api/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedCategory)));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Updated Category")))
                .andExpect(jsonPath("$.description", is("Updated Description")))
                .andExpect(jsonPath("$.type", is("INCOME")));

        verify(categoryService, times(1)).updateCategory(eq(1L), any(Category.class));
    }

    @Test
    void updateCategory_NonExistingCategory_ReturnsNotFound() throws Exception {
        // Given
        when(categoryService.updateCategory(eq(999L), any(Category.class)))
                .thenThrow(new CategoryNotFoundException("Category not found with id: 999"));

        // When
        ResultActions result = mockMvc.perform(put("/api/categories/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedCategory)));

        // Then
        result.andExpect(status().isNotFound());

        verify(categoryService, times(1)).updateCategory(eq(999L), any(Category.class));
    }

    @Test
    void updateCategory_InvalidCategory_ReturnsBadRequest() throws Exception {
        // Given
        Category invalidCategory = new Category();
        invalidCategory.setName("");
        invalidCategory.setDescription("Invalid Description");
        invalidCategory.setType(null);

        // When
        ResultActions result = mockMvc.perform(put("/api/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidCategory)));

        // Then
        result.andExpect(status().isBadRequest());

        // Verify that service was never called due to validation failure
        verify(categoryService, never()).updateCategory(eq(1L), any(Category.class));
    }

    @Test
    void updateCategory_DuplicateName_ReturnsBadRequest() throws Exception {
        // Given
        when(categoryService.updateCategory(eq(1L), any(Category.class)))
                .thenThrow(new DuplicateCategoryException("Category with name 'Updated Category' already exists"));

        // When
        ResultActions result = mockMvc.perform(put("/api/categories/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedCategory)));

        // Then
        result.andExpect(status().isBadRequest());

        verify(categoryService, times(1)).updateCategory(eq(1L), any(Category.class));
    }

    @Test
    void deleteCategory_ExistingCategory_ReturnsNoContent() throws Exception {
        // Given
        doNothing().when(categoryService).deleteCategory(1L);

        // When
        ResultActions result = mockMvc.perform(delete("/api/categories/1"));

        // Then
        result.andExpect(status().isNoContent());

        verify(categoryService, times(1)).deleteCategory(1L);
    }

    @Test
    void deleteCategory_NonExistingCategory_ReturnsNotFound() throws Exception {
        // Given
        doThrow(new CategoryNotFoundException("Category not found with id: 999"))
                .when(categoryService).deleteCategory(999L);

        // When
        ResultActions result = mockMvc.perform(delete("/api/categories/999"));

        // Then
        result.andExpect(status().isNotFound());

        verify(categoryService, times(1)).deleteCategory(999L);
    }
}
