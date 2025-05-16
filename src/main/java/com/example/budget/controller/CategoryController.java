package com.example.budget.controller;

import com.example.budget.entity.Category;
import com.example.budget.entity.CategoryType;
import com.example.budget.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Category", description = "Category management APIs")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Get all categories
     *
     * @return list of all categories
     */
    @Operation(summary = "Get all categories", description = "Returns a list of all categories")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of categories"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.findAll();
        return ResponseEntity.ok(categories);
    }

    /**
     * Get category by id
     *
     * @param id the id of the category to retrieve
     * @return the category with the given id
     */
    @Operation(summary = "Get category by ID", description = "Returns a single category by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the category"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Category> getCategoryById(
            @Parameter(description = "ID of the category to retrieve", required = true)
            @PathVariable Long id) {
        Category category = categoryService.findById(id);
        return ResponseEntity.ok(category);
    }

    /**
     * Get categories by type
     *
     * @param type the type of categories to retrieve (INCOME or EXPENSE)
     * @return list of categories with the given type
     */
    @Operation(summary = "Get categories by type", description = "Returns a list of categories with the specified type (INCOME or EXPENSE)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved the categories"),
        @ApiResponse(responseCode = "400", description = "Invalid category type"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/type/{type}")
    public ResponseEntity<List<Category>> getCategoriesByType(
            @Parameter(description = "Type of the categories to retrieve (INCOME or EXPENSE)", required = true)
            @PathVariable CategoryType type) {
        List<Category> categories = categoryService.findByType(type);
        return ResponseEntity.ok(categories);
    }

    /**
     * Create a new category
     *
     * @param category the category to create
     * @return the created category
     */
    @Operation(summary = "Create a new category", description = "Creates a new category and returns the created category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Category successfully created"),
        @ApiResponse(responseCode = "400", description = "Invalid input data or duplicate category name"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public ResponseEntity<Category> createCategory(
            @Parameter(description = "Category object to be created", required = true)
            @Valid @RequestBody Category category) {
        Category createdCategory = categoryService.createCategory(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);
    }

    /**
     * Update an existing category
     *
     * @param id the id of the category to update
     * @param category the updated category data
     * @return the updated category
     */
    @Operation(summary = "Update a category", description = "Updates an existing category and returns the updated category")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category successfully updated"),
        @ApiResponse(responseCode = "400", description = "Invalid input data or duplicate category name"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Category> updateCategory(
            @Parameter(description = "ID of the category to update", required = true)
            @PathVariable Long id, 
            @Parameter(description = "Updated category object", required = true)
            @Valid @RequestBody Category category) {
        Category updatedCategory = categoryService.updateCategory(id, category);
        return ResponseEntity.ok(updatedCategory);
    }

    /**
     * Delete a category
     *
     * @param id the id of the category to delete
     * @return no content response
     */
    @Operation(summary = "Delete a category", description = "Deletes a category by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Category successfully deleted"),
        @ApiResponse(responseCode = "404", description = "Category not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "ID of the category to delete", required = true)
            @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}