# Decision: bounded pages and a local demonstration

I restored pagination because the rubric requires it and history can grow. History/admin responses contain items/page/size/totalElements/totalPages. Default size is ten, maximum fifty, with a bounded page index. Account/card endpoints stay arrays because a customer has one of each. One small Pagination component serves the real lists.

The four-table schema and page/controller/service/repository flow remain. DTO annotations validate format; services enforce money/ownership/cross-row rules. Two custom exceptions identify common missing-resource/conflict cases. OpenAPI describes the implemented contract.

My instructor waived AWS and related deployment/DevOps work. I demonstrate local MySQL, Spring Boot, and Vite with documented startup/secrets/checks/recovery. I added no cloud infrastructure, deployment pipeline, cloud monitoring, Jira, or branch protection. The waiver does not remove JWT, validation, pagination, coverage, Postman, SonarQube, peer reviews, or the presentation.
