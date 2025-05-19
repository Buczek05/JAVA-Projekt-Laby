package com.example.budget.controller;

import com.example.budget.entity.Category;
import com.example.budget.entity.CategoryType;
import com.example.budget.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CategoryControllerTest extends BaseControllerTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void getAllCategories_ReturnsAllCategories() throws Exception {
        // Given
        Category category1 = createTestCategory("Test Category 1", "Description 1", CategoryType.EXPENSE);
        Category category2 = createTestCategory("Test Category 2", "Description 2", CategoryType.INCOME);

        // When
        ResultActions result = mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
              .andExpect(jsonPath("$[*].name", hasItems("Test Category 1", "Test Category 2")))
              .andExpect(jsonPath("$[*].description", hasItems("Description 1", "Description 2")))
              .andExpect(jsonPath("$[*].type", hasItems("EXPENSE", "INCOME")));
    }

    @Test
    void getCategoryById_ExistingId_ReturnsCategory() throws Exception {
        // Given
        Category category = createTestCategory("Test Category", "Test Description", CategoryType.EXPENSE);

        // When
        ResultActions result = mockMvc.perform(get("/api/categories/{id}", category.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.id", is(category.getId().intValue())))
              .andExpect(jsonPath("$.name", is("Test Category")))
              .andExpect(jsonPath("$.description", is("Test Description")))
              .andExpect(jsonPath("$.type", is("EXPENSE")));
    }

    @Test
    void getCategoryById_NonExistingId_ReturnsNotFound() throws Exception {
        // When
        ResultActions result = mockMvc.perform(get("/api/categories/{id}", 999)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNotFound());
    }

    @Test
    void getCategoriesByType_ReturnsMatchingCategories() throws Exception {
        // Given
        Category category1 = createTestCategory("Expense Category 1", "Description 1", CategoryType.EXPENSE);
        Category category2 = createTestCategory("Expense Category 2", "Description 2", CategoryType.EXPENSE);
        Category category3 = createTestCategory("Income Category", "Description 3", CategoryType.INCOME);

        // When
        ResultActions result = mockMvc.perform(get("/api/categories/type/{type}", "EXPENSE")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
              .andExpect(jsonPath("$[*].name", hasItems("Expense Category 1", "Expense Category 2")))
              .andExpect(jsonPath("$[*].type", everyItem(is("EXPENSE"))));
    }

    @Test
    void createCategory_ValidCategory_ReturnsCreatedCategory() throws Exception {
        // Given
        Category category = new Category();
        category.setName("New Category");
        category.setDescription("New Description");
        category.setType(CategoryType.INCOME);

        // When
        ResultActions result = mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(category)));

        // Then
        result.andExpect(status().isCreated())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.id", notNullValue()))
              .andExpect(jsonPath("$.name", is("New Category")))
              .andExpect(jsonPath("$.description", is("New Description")))
              .andExpect(jsonPath("$.type", is("INCOME")));
    }

    @Test
    void createCategory_DuplicateName_ReturnsConflict() throws Exception {
        // Given
        Category existingCategory = createTestCategory("Existing Category", "Description", CategoryType.EXPENSE);

        Category newCategory = new Category();
        newCategory.setName("Existing Category"); // Same name as existing category
        newCategory.setDescription("New Description");
        newCategory.setType(CategoryType.INCOME);

        // When
        ResultActions result = mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newCategory)));

        // Then
        result.andExpect(status().isConflict());
    }

    @Test
    void updateCategory_ValidCategory_ReturnsUpdatedCategory() throws Exception {
        // Given
        Category category = createTestCategory("Original Category", "Original Description", CategoryType.EXPENSE);

        Category updatedCategory = new Category();
        updatedCategory.setName("Updated Category");
        updatedCategory.setDescription("Updated Description");
        updatedCategory.setType(CategoryType.INCOME);

        // When
        ResultActions result = mockMvc.perform(put("/api/categories/{id}", category.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedCategory)));

        // Then
        result.andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.id", is(category.getId().intValue())))
              .andExpect(jsonPath("$.name", is("Updated Category")))
              .andExpect(jsonPath("$.description", is("Updated Description")))
              .andExpect(jsonPath("$.type", is("INCOME")));
    }

    @Test
    void updateCategory_NonExistingId_ReturnsNotFound() throws Exception {
        // Given
        Category updatedCategory = new Category();
        updatedCategory.setName("Updated Category");
        updatedCategory.setDescription("Updated Description");
        updatedCategory.setType(CategoryType.INCOME);

        // When
        ResultActions result = mockMvc.perform(put("/api/categories/{id}", 999)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedCategory)));

        // Then
        result.andExpect(status().isNotFound());
    }

    @Test
    void deleteCategory_ExistingId_ReturnsNoContent() throws Exception {
        // Given
        Category category = createTestCategory("Category to Delete", "Description", CategoryType.EXPENSE);

        // When
        ResultActions result = mockMvc.perform(delete("/api/categories/{id}", category.getId())
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNoContent());

        // Verify category is deleted
        mockMvc.perform(get("/api/categories/{id}", category.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCategory_NonExistingId_ReturnsNotFound() throws Exception {
        // When
        ResultActions result = mockMvc.perform(delete("/api/categories/{id}", 999)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNotFound());
    }

    private Category createTestCategory(String name, String description, CategoryType type) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setType(type);
        return categoryRepository.save(category);
    }
}
