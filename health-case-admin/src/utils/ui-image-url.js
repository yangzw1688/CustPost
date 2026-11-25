export function adminUiImageUrl(url) {
  if (url === '/images/caringhealth.png') {
    return `${process.env.BASE_URL}images/caringhealth.png`
  }
  return url
}
