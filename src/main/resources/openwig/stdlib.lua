assert = function (c, msg)
	if not c then
		error(msg or "assertion failed!")
	end
	return c
end

ipairs = function (t)
	local f = function (t, i)
		i = i + 1
		local v = t[i]
		if v ~= nil then
			return i, v
		end
	end
	return f, t, 0
end

pairs = function (t)
	return next, t, nil
end

local sort = function (array, comp)
	assert(type(array) == "table", "array expected")
	if comp then
		assert(type(comp) == "function", "comp expected")
	else
		comp = function (a, b)
			return a < b
		end
	end
	local n = #array
	for i = 1, n - 1 do
		local min = i
		for j = i + 1, n do
			if comp(array[j], array[min]) then
				min = j
			end
		end

		if min ~= i then
			local tmp = array[i]
			array[i] = array[min]
			array[min] = tmp
		end
	end
end

table.sort = sort

string.len = function (s)
	return #s
end

local concat = table.concat
string.rep = function (s, n)
	local t = {}
	for i = 1, n do
		t[i] = s
	end
	return concat(t)
end

string.gmatch = function (s, pattern)
	local i = 0
	return function ()
		local len = string.len(s)
		if i < len then
			local startindex, endindex = string.find(s, pattern, i + 1)
			if startindex then
				i = endindex
				return string.match(s, pattern, startindex)
			end
		end
	end
end

math.max = function (first, ...)
	local max = first
	local n = select("#", ...)
	for i = 1, n do
		local v = select(i, ...)
		if max < v then
			max = v
		end
	end
	return max
end

math.min = function (first, ...)
	local min = first
	local n = select("#", ...)
	for i = 1, n do
		local v = select(i, ...)
		if min > v then
			min = v
		end
	end
	return min
end

local error = error
local create = coroutine.create
local resume = coroutine.resume

coroutine.wrap = function (f)
	local c = create(f)
	return function (...)
		local res = { resume(c, ...) }
		if res[1] then
			return unpack(res, 2)
		else
			error(res[2])
		end
	end
end

package = {}
package.loaded = {
	string = string,
	table = table,
	math = math,
	package = package,
	os = os,
	coroutine = coroutine,
	io = {}
}

package.loaders = {}
package.path = "/"

table.insert(package.loaders, bytecodeloader)
bytecodeloader = nil

require = function (modname)
	local loaded = package.loaded[modname]
	if not loaded then
		local loaders = package.loaders
		local err = ""
		for i = 1, #loaders do
			local loader = loaders[i]
			if type(loader) == "function" then
				local res = loader(modname)
				if type(res) == "function" then
					package.loaded[modname] = res
					return res(modname)
				elseif type(res) == "string" then
					err = err .. res
				end
			end
		end
		error("Module '" .. modname .. "' not found:" .. err)
	else
		return loaded
	end
end

package.seeall = function (module)
	local meta = getmetatable(module)
	if not meta then
		meta = {}
		meta.__index = getfenv(0)
		setmetatable(module, meta)
	end
end

local seeall = package.seeall

module = function (name, ...)
	local env = getfenv(0)
	local loaded = package.loaded[name]
	if not loaded then
		loaded = env[name]
	end
	if not loaded then
		loaded = {}
		package.loaded[name] = loaded
		loaded._NAME = name
		loaded._M = loaded

		local pkg, moduleName = name:match("^(.*%.)([^.]*)$")
		loaded._PACKAGE = pkg
		if name:find(".", 1, true) then
			local t = env
			for comp in name:gmatch("([^%.]*)%.") do
				if not t[comp] then
					t[comp] = {}
				end
				t = t[comp]
			end
			t[moduleName] = loaded
		else
			env[name] = loaded
		end
	end

	seeall(loaded)
	local argCount = select("#", ...)
	for i = 1, argCount do
		local option = select(i, ...)
		option(loaded)
	end
	setfenv(2, loaded)
end

withproperties = function (table)
	local meta = getmetatable(table)
	local index = meta and meta.__index
	local getter = (type(index) == "function")
	local propIndex = function (t, key)
		local prop = getter and index(t, key) or (index and index[key])
		if prop ~= nil then
			return prop
		end
		local setter = t["get" .. key] or t["is" .. key]
		if setter then
			return setter(t)
		end
		return nil
	end

	local propNewIndex = function (t, key, val)
		local setter = t["set" .. key]
		if setter then
			setter(t, val)
		else
			t[key] = val
		end
	end

	setmetatable(table, {
		__index = propIndex,
		__newindex = propNewIndex
	})
	return table
end

table.getn = function (t)
	return #t
end

string.gfind = string.gmatch

math.mod = function (a, b)
	return a % b
end
