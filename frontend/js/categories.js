document.addEventListener('DOMContentLoaded', () => {
  if (!window.SCHEL.requireAuth()) {
    return;
  }

  const categoryList = document.getElementById('category-list');
  const categorySearch = document.getElementById('category-search');
  const categoryCountBadge = document.getElementById('category-count-badge');
  const categoryModal = document.getElementById('category-modal');
  const deleteCategoryModal = document.getElementById('delete-category-modal');
  const categoryForm = document.getElementById('category-form');
  const toastContainer = document.getElementById('toast-container');

  const state = {
    categories: [],
    pendingEditId: null,
    pendingDeleteId: null
  };

  const showToast = (message, type = 'success') => {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    toastContainer.appendChild(toast);

    setTimeout(() => toast.classList.add('is-visible'), 20);
    setTimeout(() => {
      toast.classList.remove('is-visible');
      setTimeout(() => toast.remove(), 220);
    }, 2600);
  };

  const clearFormErrors = () => {
    document.querySelectorAll('.field-error').forEach((node) => {
      node.textContent = '';
    });
  };

  const buildEmptyState = (title, message, actionText, actionHandler) => {
    const wrapper = document.createElement('div');
    wrapper.className = 'empty-state';
    const icon = document.createElement('div');
    icon.className = 'empty-icon';
    icon.textContent = '🏷️';

    const heading = document.createElement('h3');
    heading.textContent = title;

    const description = document.createElement('p');
    description.textContent = message;

    const button = document.createElement('button');
    button.className = 'btn btn-primary';
    button.type = 'button';
    button.textContent = actionText;
    button.addEventListener('click', actionHandler);

    wrapper.append(icon, heading, description, button);
    return wrapper;
  };

  const renderLoading = () => {
    categoryList.innerHTML = '';
    const skeletons = Array.from({ length: 3 }, () => {
      const card = document.createElement('div');
      card.className = 'category-card loading-card';
      card.innerHTML = `
        <div class="load-skeleton" style="height: 18px; width: 42%; margin-bottom: 14px;"></div>
        <div class="load-skeleton" style="height: 12px; width: 72%; margin-bottom: 18px;"></div>
        <div class="load-skeleton" style="height: 32px; width: 120px; border-radius: 999px;"></div>
      `;
      return card;
    });
    skeletons.forEach((card) => categoryList.appendChild(card));
  };

  const renderCategoryList = async () => {
    const searchTerm = (categorySearch.value || '').trim().toLowerCase();
    const visible = state.categories.filter((category) => {
      const haystack = `${category.name || ''}`.toLowerCase();
      return !searchTerm || haystack.includes(searchTerm);
    });

    categoryCountBadge.textContent = `${visible.length} ${visible.length === 1 ? 'category' : 'categories'}`;
    categoryList.innerHTML = '';

    if (visible.length === 0) {
      const hasData = state.categories.length > 0;
      const title = hasData ? 'No matching categories' : 'No categories yet';
      const message = hasData ? 'Try a different search.' : 'Create categories to keep your schedules organized.';
      const buttonText = hasData ? 'Clear Search' : '+ Create Category';
      const actionHandler = hasData ? () => {
        categorySearch.value = '';
        renderCategoryList();
      } : () => openCreateModal();
      categoryList.appendChild(buildEmptyState(title, message, buttonText, actionHandler));
      return;
    }

    for (const category of visible) {
      const card = document.createElement('article');
      card.className = 'category-card';
      card.innerHTML = `
        <div class="category-header">
          <div class="category-title-wrap">
            <span class="category-icon" style="background:${category.color || '#5b6cff'};"></span>
            <div>
              <h3>${category.name || 'Untitled category'}</h3>
            </div>
          </div>
        </div>
        <div class="category-body">
          <p>Color ${category.color || 'Not set'}</p>
        </div>
        <div class="category-footer">
          <button class="btn btn-secondary btn-small" type="button" data-edit-id="${category.id}">Edit</button>
          <button class="btn btn-danger btn-small" type="button" data-delete-id="${category.id}">Delete</button>
        </div>
      `;

      card.querySelector('[data-edit-id]').addEventListener('click', () => openEditModal(category.id));
      card.querySelector('[data-delete-id]').addEventListener('click', () => openDeleteModal(category.id));
      categoryList.appendChild(card);
    }
  };

  const openCreateModal = () => {
    clearFormErrors();
    categoryForm.reset();
    document.getElementById('category-modal-title').textContent = 'Create Category';
    document.getElementById('save-category-btn').textContent = 'Create Category';
    document.getElementById('category-color').value = '#5b6cff';
    state.pendingEditId = null;
    categoryModal.classList.add('open');
    categoryModal.setAttribute('aria-hidden', 'false');
  };

  const openEditModal = (id) => {
    const category = state.categories.find((item) => item.id === id);
    if (!category) return;
    clearFormErrors();
    state.pendingEditId = id;
    document.getElementById('category-modal-title').textContent = 'Edit Category';
    document.getElementById('save-category-btn').textContent = 'Update Category';
    document.getElementById('category-name').value = category.name || '';
    document.getElementById('category-color').value = category.color || '#5b6cff';
    categoryModal.classList.add('open');
    categoryModal.setAttribute('aria-hidden', 'false');
  };

  const closeModal = (modal) => {
    modal.classList.remove('open');
    modal.setAttribute('aria-hidden', 'true');
  };

  const closeAllModals = () => {
    closeModal(categoryModal);
    closeModal(deleteCategoryModal);
  };

  const openDeleteModal = (id) => {
    const category = state.categories.find((item) => item.id === id);
    if (!category) return;
    state.pendingDeleteId = id;
    document.getElementById('delete-category-message').textContent = `Are you sure you want to delete "${category.name || 'this category'}"?`;
    deleteCategoryModal.classList.add('open');
    deleteCategoryModal.setAttribute('aria-hidden', 'false');
  };

  const validateCategoryPayload = (payload) => {
    const errors = {};
    const name = (payload.name || '').trim();
    if (!name) {
      errors.name = 'Category name is required.';
    } else if (name.length > 100) {
      errors.name = 'Category name cannot exceed 100 characters.';
    }
    if (payload.color && !/^#[0-9A-Fa-f]{3}([0-9A-Fa-f]{3})?$/.test(payload.color)) {
      errors.color = 'Please provide a valid color.';
    }
    return errors;
  };

  const applyFormErrors = (errors) => {
    clearFormErrors();
    Object.entries(errors).forEach(([key, value]) => {
      const field = document.querySelector(`[data-error-for="${key}"]`);
      if (field) field.textContent = value;
    });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const saveButton = document.getElementById('save-category-btn');
    if (saveButton.disabled) return;

    const formData = new FormData(categoryForm);
    const payload = {
      name: formData.get('name') || '',
      color: formData.get('color') || '#5b6cff'
    };

    const errors = validateCategoryPayload(payload);
    if (Object.keys(errors).length) {
      applyFormErrors(errors);
      return;
    }

    const isEditing = Boolean(state.pendingEditId);
    const originalButtonText = saveButton.textContent;
    saveButton.disabled = true;
    saveButton.classList.add('is-loading');
    saveButton.textContent = isEditing ? 'Updating…' : 'Creating…';

    try {
      let result;
      if (isEditing) {
        result = await window.SCHEL.api.updateCategory(state.pendingEditId, payload);
        if (!result?.category?.id) {
          throw new Error('The server did not return the updated category.');
        }
        state.categories = state.categories.map((category) => (
          category.id === result.category.id ? result.category : category
        ));
        showToast('Category updated successfully.', 'success');
      } else {
        result = await window.SCHEL.api.createCategory(payload);
        if (!result?.category?.id) {
          throw new Error('The server did not return the created category.');
        }
        state.categories.push(result.category);
        showToast('Category created successfully.', 'success');
      }

      categoryForm.reset();
      clearFormErrors();
      state.pendingEditId = null;
      closeModal(categoryModal);
      await renderCategoryList();
    } catch (error) {
      showToast(error?.message || 'Could not save this category.', 'error');
    } finally {
      saveButton.disabled = false;
      saveButton.classList.remove('is-loading');
      saveButton.textContent = originalButtonText;
    }
  };

  const handleDelete = async () => {
    if (!state.pendingDeleteId) return;
    const deleteButton = document.getElementById('confirm-delete-category-btn');
    if (deleteButton.disabled) return;

    const originalButtonText = deleteButton.textContent;
    deleteButton.disabled = true;
    deleteButton.classList.add('is-loading');
    deleteButton.textContent = 'Deleting…';

    try {
      const categoryId = state.pendingDeleteId;
      await window.SCHEL.api.deleteCategory(categoryId);
      state.categories = state.categories.filter((category) => category.id !== categoryId);
      showToast('Category deleted successfully.', 'success');
      closeModal(deleteCategoryModal);
      state.pendingDeleteId = null;
      await renderCategoryList();
    } catch (error) {
      showToast(error?.message || 'Could not delete this category.', 'error');
    } finally {
      deleteButton.disabled = false;
      deleteButton.classList.remove('is-loading');
      deleteButton.textContent = originalButtonText;
    }
  };

  const refreshCategories = async () => {
    renderLoading();
    try {
      const result = await window.SCHEL.api.getCategories();
      if (!Array.isArray(result?.categories)) {
        throw new Error('The server returned an invalid categories response.');
      }
      state.categories = result.categories;
      await renderCategoryList();
    } catch (error) {
      categoryList.innerHTML = '';
      categoryList.appendChild(buildEmptyState('Unable to load categories', error?.message || 'Please try again in a moment.', 'Try Again', refreshCategories));
      showToast(error?.message || 'Unable to load categories.', 'error');
    }
  };

  categorySearch.addEventListener('input', renderCategoryList);
  categoryForm.addEventListener('submit', handleSubmit);
  document.getElementById('open-create-modal').addEventListener('click', openCreateModal);
  document.getElementById('confirm-delete-category-btn').addEventListener('click', handleDelete);

  document.querySelectorAll('[data-close-modal]').forEach((button) => {
    button.addEventListener('click', () => {
      closeAllModals();
    });
  });

  const logoutButton = document.querySelector('[data-action="logout"]');
  if (logoutButton) {
    logoutButton.addEventListener('click', () => {
      window.SCHEL.logout();
    });
  }

  refreshCategories();
});
