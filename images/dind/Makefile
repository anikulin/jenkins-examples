DOCKER_REGISTRY ?= cr.yandex/...

NS := jenkins
NAME := dind

VERSION ?= 1.0

INFO_FILE ?= target/image_info

TAG := ${VERSION}

FULL_NAME := ${DOCKER_REGISTRY}/${NS}/${NAME}

IMG_NAME    := ${FULL_NAME}:${TAG}
LATEST := ${FULL_NAME}:latest

DOCKER_BUILD_OPTIONS ?= --network=host

.PHONY: build push save_info

build:
	docker build ${DOCKER_BUILD_OPTIONS} -t ${IMG_NAME} .

save_info:
	echo ${TAG} > ${INFO_FILE}

push:
	docker tag ${IMG_NAME} ${LATEST}

	docker push ${IMG_NAME}
	docker push ${LATEST}
