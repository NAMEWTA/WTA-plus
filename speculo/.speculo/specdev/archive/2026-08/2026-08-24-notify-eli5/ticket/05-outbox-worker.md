# T-05 Outbox Worker

状态：done

已实现短事务 `FOR UPDATE SKIP LOCKED` 领取、租约、指数退避、最大尝试次数、Dead Letter 和未知状态等待回执；Provider I/O 位于领取事务之外。
