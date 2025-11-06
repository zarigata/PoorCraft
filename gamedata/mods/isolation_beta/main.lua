local mod = {}

function mod.init()
  local observed = tostring(_G.isolation_probe)
  api.set_shared_data("isolation_beta_observed", observed)
  _G.isolation_probe = "beta"
  api.set_shared_data("isolation_beta_written", _G.isolation_probe)
end

return mod
