import request from '../utils/request'

// 员工管理
export const employeeApi = {
  list: (params: any) => request.get('/system/employees', { params }),
  getById: (id: number) => request.get(`/system/employees/${id}`),
  create: (data: any) => request.post('/system/employees', data),
  update: (id: number, data: any) => request.put(`/system/employees/${id}`, data),
  updateStatus: (id: number, status: number) => request.put(`/system/employees/${id}/status`, null, { params: { status } }),
  resetPassword: (id: number, newPassword = '123456') => request.post(`/system/employees/${id}/reset-password`, null, { params: { newPassword } }),
  delete: (id: number) => request.delete(`/system/employees/${id}`),
}

// 角色管理
export const roleApi = {
  list: (params?: any) => request.get('/system/roles', { params }),
  getById: (id: number) => request.get(`/system/roles/${id}`),
  create: (data: any) => request.post('/system/roles', data),
  update: (id: number, data: any) => request.put(`/system/roles/${id}`, data),
  delete: (id: number) => request.delete(`/system/roles/${id}`),
  getRoleMenus: (id: number) => request.get(`/system/roles/${id}/menus`),
  assignMenus: (id: number, menuIds: number[]) => request.post(`/system/roles/${id}/menus`, menuIds),
}

// 菜单管理
export const menuApi = {
  list: () => request.get('/system/menus'),
  tree: () => request.get('/system/menus/tree'),
  userTree: (platform?: string) => request.get('/system/menus/user-tree', { params: { platform } }),
  getById: (id: number) => request.get(`/system/menus/${id}`),
  create: (data: any) => request.post('/system/menus', data),
  update: (id: number, data: any) => request.put(`/system/menus/${id}`, data),
  delete: (id: number) => request.delete(`/system/menus/${id}`),
}
