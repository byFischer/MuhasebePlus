import { create } from "zustand";

export const useEditMode = create((set) => ({
  isEditMode: false,
  isDirty: false,
  pendingPositions: [],
  enterEdit: () => set({ isEditMode: true }),
  exitEdit: () => set({ isEditMode: false, isDirty: false, pendingPositions: [] }),
  setPositions: (positions) => set({ pendingPositions: positions, isDirty: true }),
  clearDirty: () => set({ isDirty: false }),
}));
