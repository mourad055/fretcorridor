export const environment = {
  production: false,
  // Relatif : web-demo (:8099) et ng serve proxyifient /api/ vers le gateway
  // (cf web/nginx.conf). localhost:8082 direct reste possible via ng serve
  // sans Nginx en surchargeant cette variable au build.
  apiBaseUrl: '/api/v1',
  enableDemoLogin: true,
};
