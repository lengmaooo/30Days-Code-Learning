#!/bin/bash

# 缓存穿透测试脚本
# 使用方法：./test-cache-penetration.sh

echo "========================================"
echo "缓存穿透测试"
echo "========================================"

# 测试1：查询不存在的用户ID（无防护），观察每次都会查DB
echo ""
echo "【测试1】缓存穿透 - 无防护版本"
echo "连续查询不存在的用户ID（-1），观察每次都会查DB"
echo "----------------------------------------"

for i in {1..5}; do
  echo ""
  echo "第 $i 次查询："
  curl -s "http://localhost:8080/cache-test/penetration?id=-1"
  echo ""
  sleep 1
done

echo ""
echo "========================================"
echo "【测试1 结论】"
echo "如果没有防护，每次查询不存在的用户ID都会查DB，导致缓存穿透！"
echo "========================================"

sleep 3

# 测试2：使用空值缓存防护
echo ""
echo "【测试2】缓存穿透 - 空值缓存防护版本"
echo "连续查询不存在的用户ID（-1），观察只有第一次查DB"
echo "----------------------------------------"

for i in {1..5}; do
  echo ""
  echo "第 $i 次查询："
  curl -s "http://localhost:8080/cache-test/penetration-protected?id=-1"
  echo ""
  sleep 1
done

echo ""
echo "========================================"
echo "【测试2 结论】"
echo "使用空值缓存防护后，只有第一次查DB，后续请求直接走缓存，解决穿透问题！"
echo "========================================"

echo ""
echo "测试完成！"
