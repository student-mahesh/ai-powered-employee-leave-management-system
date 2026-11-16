export function apiMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (error?.code === 'ERR_CANCELED') return '';
  const data = error?.response?.data;
  if (data?.fields && Object.keys(data.fields).length) return Object.values(data.fields).join(' ');
  if (!error?.response && error?.request) return 'Unable to connect to LeaveFlow. Please try again.';
  return data?.message || fallback;
}
