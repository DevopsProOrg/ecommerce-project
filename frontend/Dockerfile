FROM nginx:1.27-alpine
ENV BACKEND_UPSTREAM=http://backend:8888
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY . /usr/share/nginx/html
EXPOSE 80
