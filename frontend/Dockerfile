FROM nginx:1.29-alpine
ENV BACKEND_UPSTREAM=http://backend:8888
RUN apk upgrade --no-cache
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY . /usr/share/nginx/html
EXPOSE 80
