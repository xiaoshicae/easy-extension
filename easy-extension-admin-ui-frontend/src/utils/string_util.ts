

const fullNameToShortName = (fullName: string) => {
  const items = fullName.split('.');
  return items.length > 0 ? items[items.length - 1] : '';
};

export { fullNameToShortName };
