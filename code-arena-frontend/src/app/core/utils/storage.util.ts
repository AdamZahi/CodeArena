export function saveToStorage(key: string, value: string): void {
  // TODO: Add encryption for sensitive values if required.
  localStorage.setItem(key, value);
}

export function readFromStorage(key: string): string | null {
  // TODO: Add schema/version validation for stored values.
  return localStorage.getItem(key);
}
