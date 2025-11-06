local mod = {}

function mod.init()
  local observed = tostring(_G.isolation_probe)
  api.set_shared_data("isolation_alpha_observed", observed)
  _G.isolation_probe = "alpha"
  api.set_shared_data("isolation_alpha_written", _G.isolation_probe)
end

return mod
